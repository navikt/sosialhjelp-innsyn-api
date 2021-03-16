package no.nav.sosialhjelp.innsyn.client.fiks

import com.fasterxml.jackson.core.JsonProcessingException
import kotlinx.coroutines.runBlocking
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.fiksHeaders
import no.nav.sosialhjelp.innsyn.utils.feilmeldingUtenFnr
import no.nav.sosialhjelp.innsyn.utils.lagNavEksternRefId
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.messageUtenFnr
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.utils.typeRef
import no.nav.sosialhjelp.kotlin.utils.retry
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.lang.NonNull
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import java.util.function.Predicate


@Profile("!mock")
@Component
class FiksClientImpl(
    private val clientProperties: ClientProperties,
    private val proxiedWebClientBuilder: WebClient.Builder,
    private val fiksWebClient: WebClient = proxiedWebClientBuilder
        .baseUrl(clientProperties.fiksDigisosEndpointUrl)
        .build(),
    private val retryProperties: FiksRetryProperties,
    private val redisService: RedisService,
) : FiksClient {

    override fun hentDigisosSak(digisosId: String, token: String, useCache: Boolean): DigisosSak {
        return when {
            useCache -> hentDigisosSakFraCache(digisosId) ?: hentDigisosSakFraFiks(digisosId, token)
            else -> hentDigisosSakFraFiks(digisosId, token)
        }
    }

    private fun hentDigisosSakFraCache(digisosId: String): DigisosSak? =
        redisService.get(digisosId, DigisosSak::class.java) as DigisosSak?

    private fun hentDigisosSakFraFiks(digisosId: String, token: String): DigisosSak {
        log.debug("Forsøker å hente digisosSak fra /digisos/api/v1/soknader/$digisosId")

        val digisosSak: DigisosSak? = withRetry {
            fiksWebClient.get()
                .uri(FiksPaths.PATH_DIGISOSSAK, digisosId)
                .headers { it.addAll(fiksHeaders(clientProperties, token)) }
                .retrieve()
                .onStatus(Predicate.isEqual(HttpStatus.NOT_FOUND)) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentDigisosSak feilet - ${messageUtenFnr(e)}", e)
                        FiksNotFoundException(e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .onStatus(HttpStatus::is4xxClientError) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentDigisosSak feilet - ${messageUtenFnr(e)}", e)
                        FiksClientException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .onStatus(HttpStatus::is5xxServerError) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentDigisosSak feilet - ${messageUtenFnr(e)}", e)
                        FiksServerException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .bodyToMono<DigisosSak>()
                .block()
        }
        log.debug("Hentet DigisosSak fra Fiks")
        return digisosSak!!
            .also { lagreTilCache(digisosId, it) }
    }

    private fun lagreTilCache(id: String, digisosSakEllerDokument: Any) =
        redisService.put(id, objectMapper.writeValueAsBytes(digisosSakEllerDokument))

    override fun hentDokument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out Any>,
        token: String,
    ): Any {
        return hentDokumentFraCache(dokumentlagerId, requestedClass)
            ?: hentDokumentFraFiks(digisosId, dokumentlagerId, requestedClass, token)
    }

    private fun hentDokumentFraCache(dokumentlagerId: String, requestedClass: Class<out Any>): Any? =
        redisService.get(dokumentlagerId, requestedClass)

    private fun hentDokumentFraFiks(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out Any>,
        token: String,
    ): Any {
        log.debug("Forsøker å hente dokument fra /digisos/api/v1/soknader/nav/$digisosId/dokumenter/$dokumentlagerId")

        val dokument: Any? = withRetry {
            fiksWebClient.get()
                .uri(FiksPaths.PATH_DOKUMENT, digisosId, dokumentlagerId)
                .headers { it.addAll(fiksHeaders(clientProperties, token)) }
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentDokument feilet - ${messageUtenFnr(e)}", e)
                        FiksClientException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .onStatus(HttpStatus::is5xxServerError) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentDokument feilet - ${messageUtenFnr(e)}", e)
                        FiksServerException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .bodyToMono(requestedClass)
                .block()
        }
        log.debug("Hentet dokument (${requestedClass.simpleName}) fra Fiks, dokumentlagerId=$dokumentlagerId")
        return dokument!!
            .also { lagreTilCache(dokumentlagerId, it) }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val digisosSaker: List<DigisosSak>? = withRetry {
            fiksWebClient.get()
                .uri(FiksPaths.PATH_ALLE_DIGISOSSAKER)
                .headers { it.addAll(fiksHeaders(clientProperties, token)) }
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentAlleDigisosSaker feilet - ${messageUtenFnr(e)}", e)
                        FiksClientException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .onStatus(HttpStatus::is5xxServerError) {
                    it.createException().map { e ->
                        log.warn("Fiks - hentAlleDigisosSaker feilet - ${messageUtenFnr(e)}", e)
                        FiksServerException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                    }
                }
                .bodyToMono(typeRef<List<DigisosSak>>())
                .block()
        }
        return digisosSaker!!
    }

    override fun lastOppNyEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
        token: String,
    ) {
        log.info("Starter sending til FIKS for ettersendelse med ${files.size} filer (inkludert ettersendelse.pdf). Validering, filnavn-endring, generering av ettersendelse.pdf og kryptering er OK.")

        val body = createBodyForUpload(vedleggJson, files)

        val digisosSak = hentDigisosSakFraFiks(digisosId, token)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        val responseEntity = fiksWebClient.post()
            .uri(FiksPaths.PATH_LAST_OPP_ETTERSENDELSE, kommunenummer, digisosId, navEksternRefId)
            .headers { it.addAll(fiksHeaders(clientProperties, token)) }
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError) {
                it.createException().map { e ->
                    log.warn("Fiks - Opplasting av ettersendelse på $digisosId feilet - ${messageUtenFnr(e)}", e)
                    FiksClientException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                }
            }
            .onStatus(HttpStatus::is5xxServerError) {
                it.createException().map { e ->
                    log.warn("Fiks - Opplasting av ettersendelse på $digisosId feilet - ${messageUtenFnr(e)}", e)
                    FiksServerException(e.rawStatusCode, e.message?.feilmeldingUtenFnr, e)
                }
            }
            .toEntity<String>()
            .block()

        log.info("Sendte ettersendelse til kommune $kommunenummer i Fiks, fikk navEksternRefId $navEksternRefId (statusCode: ${responseEntity!!.statusCodeValue})")
    }

    fun createBodyForUpload(
        vedleggJson: JsonVedleggSpesifikasjon,
        files: List<FilForOpplasting>,
    ): LinkedMultiValueMap<String, Any> {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("vedlegg.json", createHttpEntityOfString(serialiser(vedleggJson), "vedlegg.json"))

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", createHttpEntityOfString(serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", createHttpEntityOfFile(file, "dokument:$fileId"))
        }
        return body
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

    private fun <T> withRetry(block: () -> T): T {
        return runBlocking {
            retry(
                attempts = retryProperties.attempts,
                initialDelay = retryProperties.initialDelay,
                maxDelay = retryProperties.maxDelay,
                retryableExceptions = arrayOf(FiksServerException::class)
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
    val storrelse: Long,
)
