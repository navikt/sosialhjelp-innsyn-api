package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.coroutines.runBlocking
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.FilForOpplasting
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.fiksHeaders
import no.nav.sbl.sosialhjelpinnsynapi.utils.feilmeldingUtenFnr
import no.nav.sbl.sosialhjelpinnsynapi.utils.lagNavEksternRefId
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.utils.toFiksErrorMessage
import no.nav.sbl.sosialhjelpinnsynapi.utils.typeRef
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.kotlin.utils.retry
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate


@Profile("!mock")
@Component
class FiksClientImpl(
        private val clientProperties: ClientProperties,
        private val restTemplate: RestTemplate,
        private val retryProperties: FiksRetryProperties,
        private val redisService: RedisService
) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl

    override fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak {
        return when {
            useCache -> hentDigisosSakFraCache(digisosId) ?: hentDigisosSakFraFiks(digisosId, token)
            else -> hentDigisosSakFraFiks(digisosId, token)
        }
    }

    private fun hentDigisosSakFraCache(digisosId: String): DigisosSak? =
            redisService.get(digisosId, DigisosSak::class.java) as DigisosSak?

    private fun hentDigisosSakFraFiks(digisosId: String, token: String): DigisosSak {
        log.debug("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")

        try {
            val headers = fiksHeaders(clientProperties, token)
            val urlTemplate = baseUrl + FiksPaths.PATH_DIGISOSSAK

            val response: ResponseEntity<String> = withRetry {
                restTemplate.exchange(urlTemplate, HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java, digisosId)
            }

            log.debug("Hentet DigisosSak fra Fiks, digisosId=$digisosId")
            val body = response.body!!
            return objectMapper.readValue(body, DigisosSak::class.java)
                    .also { lagreTilCache(digisosId, it) }
        } catch (e: HttpClientErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDigisosSak feilet - $message - $fiksErrorMessage", e)
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw FiksNotFoundException(message, e)
            }
            throw FiksClientException(e.rawStatusCode, e.message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDigisosSak feilet - $message - $fiksErrorMessage", e)
            throw FiksServerException(e.rawStatusCode, message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentDigisosSak feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    private fun lagreTilCache(id: String, digisosSakEllerDokument: Any) =
            redisService.put(id, objectMapper.writeValueAsBytes(digisosSakEllerDokument))

    override fun hentDokument(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        return hentDokumentFraCache(dokumentlagerId, requestedClass)
                ?: hentDokumentFraFiks(digisosId, dokumentlagerId, requestedClass, token)
    }

    private fun hentDokumentFraCache(dokumentlagerId: String, requestedClass: Class<out Any>): Any? =
            redisService.get(dokumentlagerId, requestedClass)

    private fun hentDokumentFraFiks(digisosId: String, dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        log.debug("Forsøker å hente dokument fra $baseUrl/digisos/api/v1/soknader/nav/$digisosId/dokumenter/$dokumentlagerId")

        try {
            val headers = fiksHeaders(clientProperties, token)
            val urlTemplate = baseUrl + FiksPaths.PATH_DOKUMENT
            val vars = mapOf("digisosId" to digisosId, "dokumentlagerId" to dokumentlagerId)

            val response: ResponseEntity<String> = withRetry {
                restTemplate.exchange(urlTemplate, HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java, vars)
            }

            log.debug("Hentet dokument (${requestedClass.simpleName}) fra Fiks, dokumentlagerId=$dokumentlagerId")
            return objectMapper.readValue(response.body!!, requestedClass)
                    .also { lagreTilCache(dokumentlagerId, it) }
        } catch (e: HttpClientErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDokument feilet - $message - $fiksErrorMessage", e)
            throw FiksClientException(e.rawStatusCode, message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentDokument feilet - $message - $fiksErrorMessage", e)
            throw FiksServerException(e.rawStatusCode, message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentDokument feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        try {
            val headers = fiksHeaders(clientProperties, token)
            val url = baseUrl + FiksPaths.PATH_ALLE_DIGISOSSAKER

            val response: ResponseEntity<List<DigisosSak>> = withRetry {
                restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<DigisosSak>>())
            }
            return response.body.orEmpty()
        } catch (e: HttpClientErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentAlleDigisosSaker feilet - $message - $fiksErrorMessage", e)
            throw FiksClientException(e.rawStatusCode, message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Fiks - hentAlleDigisosSaker feilet - $message - $fiksErrorMessage", e)
            throw FiksServerException(e.rawStatusCode, message, e)
        } catch (e: Exception) {
            log.warn("Fiks - hentAlleDigisosSaker feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    override fun lastOppNyEttersendelse(files: List<FilForOpplasting>, vedleggJson: JsonVedleggSpesifikasjon, digisosId: String, token: String) {
        log.info("Starter sending av ettersendelse med ${files.size} filer")
        val headers = fiksHeaders(clientProperties, token)
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add("vedlegg.json", createHttpEntityOfString(serialiser(vedleggJson), "vedlegg.json"))

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", createHttpEntityOfString(serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", createHttpEntityOfFile(file, "dokument:$fileId"))
        }

        val digisosSak = hentDigisosSakFraFiks(digisosId, token)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        val requestEntity = HttpEntity(body, headers)
        try {
            val urlTemplate = "$baseUrl/digisos/api/v1/soknader/{kommunenummer}/{digisosId}/{navEksternRefId}"
            val vars = mapOf("kommunenummer" to kommunenummer, "digisosId" to digisosId, "navEksternRefId" to navEksternRefId)
            val response: ResponseEntity<String> = restTemplate.exchange(urlTemplate, HttpMethod.POST, requestEntity, String::class.java, vars)

            log.info("Sendte ettersendelse til kommune $kommunenummer i Fiks, fikk navEksternRefId $navEksternRefId (statusCode: ${response.statusCodeValue})")

        } catch (e: HttpClientErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Opplasting av ettersendelse på $digisosId feilet - $message - $fiksErrorMessage", e)
            throw FiksClientException(e.rawStatusCode, message, e)
        } catch (e: HttpServerErrorException) {
            val fiksErrorMessage = e.toFiksErrorMessage()?.feilmeldingUtenFnr
            val message = e.message?.feilmeldingUtenFnr
            log.warn("Opplasting av ettersendelse på $digisosId feilet - $message - $fiksErrorMessage", e)
            throw FiksServerException(e.rawStatusCode, message, e)
        } catch (e: Exception) {
            log.warn("Opplasting av ettersendelse på $digisosId feilet", e)
            throw FiksException(e.message?.feilmeldingUtenFnr, e)
        }
    }

    fun createHttpEntityOfString(body: String, name: String): HttpEntity<Any> {
        return createHttpEntity(body, name, null, "text/plain;charset=UTF-8")
    }

    fun createHttpEntityOfFile(file: FilForOpplasting, name: String): HttpEntity<Any> {
        return createHttpEntity(InputStreamResource(file.fil), name, file.filnavn, "application/octet-stream")
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

    fun serialiser(@NonNull metadata: Any): String {
        try {
            return objectMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }
    }

    private fun <T> withRetry(block: () -> ResponseEntity<T>): ResponseEntity<T> {
        return runBlocking {
            retry(
                    attempts = retryProperties.attempts,
                    initialDelay = retryProperties.initialDelay,
                    maxDelay = retryProperties.maxDelay,
                    retryableExceptions = arrayOf(HttpServerErrorException::class)
            ) {
                block()
            }
        }
    }

    companion object {
        private val log by logger()
    }
}

data class VedleggMetadata(
        val filnavn: String?,
        val mimetype: String?,
        val storrelse: Long
)
