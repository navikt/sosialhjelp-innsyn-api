package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClientImpl
import no.nav.sbl.sosialhjelpinnsynapi.fiks.VedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.DigisosApiWrapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.util.*


@Profile("dev-sbs|local")
@Component
class DigisosApiClientImpl(clientProperties: ClientProperties,
                           private val restTemplate: RestTemplate,
                           private val idPortenService: IdPortenService,
                           private val fiksClientImpl: FiksClientImpl) : DigisosApiClient {

    companion object {
        val log by logger()
    }

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
        } catch (e: HttpStatusCodeException) {
            log.warn(e.responseBodyAsString)
            log.warn("Fiks - oppdaterDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw FiksException(null, e.message, e)
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
            body.add("vedleggSpesifikasjon:$fileId", fiksClientImpl.createHttpEntityOfString(fiksClientImpl.serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", fiksClientImpl.createHttpEntityOfFile(file, "dokument:$fileId"))
        }

        val requestEntity = HttpEntity(body, headers)
        try {
            val path = "$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$soknadId/filer"
            val response = restTemplate.exchange(path, HttpMethod.POST, requestEntity, String::class.java)

            val opplastingResponse: List<FilOpplastingResponse> = objectMapper.readValue(response.body!!)
            log.info("Filer sendt til Fiks")
            return opplastingResponse.map { filOpplastingResponse -> filOpplastingResponse.dokumentlagerDokumentId }

        } catch (e: HttpStatusCodeException) {
            log.warn(e.responseBodyAsString)
            log.warn("Opplasting av filer feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Opplasting av filer feilet", e)
            throw FiksException(null, e.message, e)
        }

    }

    fun opprettDigisosSak(): String? {
        val httpEntity = HttpEntity("", headers())
        try {
            val response = restTemplate.exchange("$baseUrl/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/ny?sokerFnr=26104500284", HttpMethod.POST, httpEntity, String::class.java)
            log.info("Opprettet sak hos Fiks. Digisosid: ${response.body}")
            return response.body?.replace("\"", "")
        } catch (e: HttpStatusCodeException) {
            log.warn("Fiks - opprettDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.error(e.message, e)
            throw FiksException(null, e.message, e)
        }
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
}

data class FilOpplastingResponse(
        val filnavn: String,
        val dokumentlagerDokumentId: String,
        val storrelse: Long
)
