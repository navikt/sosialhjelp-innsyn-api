package no.nav.sbl.sosialhjelpinnsynapi.client.digisosapi

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksEttersendelseClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.VedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.client.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksClientException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksServerException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.util.*


@Profile("!(prod-sbs|mock)")
@Component
class DigisosApiClientImpl(
        clientProperties: ClientProperties,
        private val restTemplate: RestTemplate,
        private val idPortenService: IdPortenService,
        private val fiksClientImpl: FiksClientImpl,
        private val fiksEttersendelseClientImpl: FiksEttersendelseClientImpl
) : DigisosApiClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonIdKommune = clientProperties.fiksIntegrasjonIdKommune
    private val fiksIntegrasjonPassordKommune = clientProperties.fiksIntegrasjonPassordKommune

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        var id = fiksDigisosId
        if (fiksDigisosId == null || fiksDigisosId == "001" || fiksDigisosId == "002" || fiksDigisosId == "003") {
            id = opprettDigisosSak()
            log.info("Laget ny digisossak: $id")
        }
        val httpEntity = HttpEntity(objectMapper.writeValueAsString(digisosApiWrapper), headers())
        try {
            restTemplate.exchange("$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$id", HttpMethod.POST, httpEntity, String::class.java)
            log.info("Postet DigisosSak til Fiks")
            return id
        } catch (e: HttpClientErrorException) {
            log.warn(e.responseBodyAsString)
            log.warn("Fiks - oppdaterDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksClientException(e.statusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            log.warn(e.responseBodyAsString)
            log.warn("Fiks - oppdaterDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksServerException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw FiksException(e.message, e)
        }
    }

    // Brukes for å laste opp Pdf-er fra test-fagsystem i q-miljø
    override fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String> {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        val accessToken = runBlocking { idPortenService.requestToken() }
        headers.set(AUTHORIZATION, "Bearer " + accessToken.token)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonIdKommune)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonPassordKommune)
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", createHttpEntityOfString(fiksEttersendelseClientImpl.serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", createHttpEntityOfFile(file,"dokument:$fileId"))
        }

        val requestEntity = HttpEntity(body, headers)
        try {
            val path = "$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$soknadId/filer"
            val response = restTemplate.exchange(path, HttpMethod.POST, requestEntity, String::class.java)

            val opplastingResponse: List<FilOpplastingResponse> = objectMapper.readValue(response.body!!)
            log.info("Filer sendt til Fiks")
            return opplastingResponse.map { filOpplastingResponse -> filOpplastingResponse.dokumentlagerDokumentId }

        } catch (e: HttpClientErrorException) {
            log.warn(e.responseBodyAsString)
            log.warn("Opplasting av filer feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksClientException(e.statusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            log.warn(e.responseBodyAsString)
            log.warn("Opplasting av filer feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksServerException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Opplasting av filer feilet", e)
            throw FiksException(e.message, e)
        }

    }

    fun opprettDigisosSak(): String? {
        val httpEntity = HttpEntity("", headers())
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/ny?sokerFnr=26104500284", HttpMethod.POST, httpEntity, String::class.java)
            log.info("Opprettet sak hos Fiks. Digisosid: ${response.body}")
            return response.body?.replace("\"", "")
        } catch (e: HttpClientErrorException) {
            log.warn("Fiks - opprettDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksClientException(e.statusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            log.warn("Fiks - opprettDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksServerException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw FiksException(e.message, e)
        }
    }

    fun createHttpEntityOfFile(file: FilForOpplasting, name: String): HttpEntity<Any> {
        return createHttpEntity(InputStreamResource(file.fil), name, file.filnavn, "application/octet-stream")
    }

    fun createHttpEntityOfString(body: String, name: String): HttpEntity<Any> {
        return createHttpEntity(body, name, null, "text/plain;charset=UTF-8")
    }

    private fun createHttpEntity(body: Any, name: String, filename: String?, contentType: String): HttpEntity<Any> {
        val headerMap = LinkedMultiValueMap<String, String>()
        val builder: ContentDisposition.Builder = ContentDisposition
                .builder("form-data")
                .name(name)
        val contentDisposition: ContentDisposition = if (filename == null) builder.build() else builder.filename(filename).build()

        headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
        return HttpEntity(body, headerMap)
    }


    private fun headers(): HttpHeaders {
        val headers = HttpHeaders()
        val accessToken = runBlocking { idPortenService.requestToken() }
        headers.accept = Collections.singletonList(MediaType.ALL)
        headers.set(HEADER_INTEGRASJON_ID, fiksIntegrasjonIdKommune)
        headers.set(HEADER_INTEGRASJON_PASSORD, fiksIntegrasjonPassordKommune)
        headers.set(AUTHORIZATION, "Bearer " + accessToken.token)
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }

    companion object {
        private val log by logger()
    }
}

data class FilOpplastingResponse(
        val filnavn: String,
        val dokumentlagerDokumentId: String,
        val storrelse: Long
)
