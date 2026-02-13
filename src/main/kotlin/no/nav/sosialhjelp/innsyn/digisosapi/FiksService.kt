package no.nav.sosialhjelp.innsyn.digisosapi

import com.fasterxml.jackson.core.JsonProcessingException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.lagNavEksternRefId
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.messageUtenFnr
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.utils.toFiksErrorMessageUtenFnr
import no.nav.sosialhjelp.innsyn.valkey.DigisosSakCacheConfig
import no.nav.sosialhjelp.innsyn.valkey.DokumentCacheConfig
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.scheduler.Schedulers

@Component
class FiksService(
    private val tilgangskontrollService: TilgangskontrollService,
    private val fiksClient: FiksClient,
    meterRegistry: MeterRegistry,
) {
    private val opplastingsteller: Counter = meterRegistry.counter("filopplasting")

    private val filTypeTeller = Counter.builder("filtype").withRegistry(meterRegistry)

    private val log by logger()

    private val requestLocks = ConcurrentHashMap<String, Mutex>()

    suspend fun getAllSoknader(): List<DigisosSak> = fiksClient.hentAlleDigisosSaker()

    suspend fun <T : Serializable> getDocument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        cacheKey: String = dokumentlagerId,
    ): T {
        val key = "$digisosId:$dokumentlagerId:${requestedClass.name}"
        val mutex = requestLocks.computeIfAbsent(key) { Mutex() }

        return try {
            mutex.withLock {
                fiksClient.hentDokument(digisosId, dokumentlagerId, requestedClass, cacheKey)
            }
        } finally {
            requestLocks.remove(key)
        }
    }

    suspend fun getSoknad(digisosId: String): DigisosSak {
        val key = "DigisosSak:$digisosId"
        val mutex = requestLocks.computeIfAbsent(key) { Mutex() }

        return try {
            mutex.withLock {
                fiksClient.hentDigisosSak(digisosId)
            }
        } finally {
            requestLocks.remove(key)
        }.also { tilgangskontrollService.verifyDigisosSakIsForCorrectUser(it) }
    }

    suspend fun uploadEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
    ) {
        log.info(
            "Starter sending til FIKS for ettersendelse med ${files.size} filer (inkludert ettersendelse.pdf)." +
                " Validering, filnavn-endring, generering av ettersendelse.pdf og kryptering er OK.",
        )

        val body = createBodyForUpload(vedleggJson, files)

        val digisosSak = getSoknad(digisosId)
        tilgangskontrollService.verifyDigisosSakIsForCorrectUser(digisosSak)
        val kommunenummer = digisosSak.kommunenummer
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        if (isPapirsoknad(digisosSak)) {
            log.info("Kommune ${digisosSak.kommunenummer} har innsyn i papirsøknader.")
        }
        val responseEntity = fiksClient.lastOppNyEttersendelse(body, kommunenummer, digisosId, navEksternRefId)
        opplastingsteller.increment()
        files.onEach { file ->
            filTypeTeller.withTag("filtype", file.mimetype ?: "Ukjent").increment()
        }
        log.info(
            "Sendte ettersendelse til kommune $kommunenummer i Fiks, " +
                "fikk navEksternRefId $navEksternRefId (statusCode: ${responseEntity.statusCode})",
        )
    }

    fun createBodyForUpload(
        vedleggJson: JsonVedleggSpesifikasjon,
        files: List<FilForOpplasting>,
    ): MultiValueMap<String, HttpEntity<*>> {
        val bodyBuilder =
            MultipartBodyBuilder().also {
                it.part("vedlegg.json", serialize(vedleggJson).toHttpEntity("vedlegg.json"))
            }

        return files
            .foldIndexed(bodyBuilder) { i, builder, file ->
                val vedleggMetadata = VedleggMetadata(file.filnavn?.value, file.mimetype, file.storrelse)
                builder.part("vedleggSpesifikasjon:$i", serialize(vedleggMetadata).toHttpEntity("vedleggSpesifikasjon:$i"))
                builder.part("dokument:$i", InputStreamResource(file.data)).headers {
                    it.contentType = MediaType.APPLICATION_OCTET_STREAM
                    it.contentDisposition =
                        ContentDisposition
                            .builder("form-data")
                            .name("dokument:$i")
                            .filename(file.filnavn?.value)
                            .build()
                }
                builder
            }.build()
    }

    fun serialize(metadata: Any): String {
        try {
            return sosialhjelpJsonMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }
    }

    private fun isPapirsoknad(digisosSak: DigisosSak): Boolean =
        digisosSak.ettersendtInfoNAV?.ettersendelser?.isEmpty() != false && digisosSak.originalSoknadNAV == null
}

/*
 * FiksClient er ansvarlig for all kommunikasjon med Fiks via WebClient.
 * Den håndterer caching, retry-logikk og feilmapping.
 * Ikke bruk direkte, men gå via FiksService.
 */
@Component
class FiksClient(
    private val fiksWebClient: WebClient,
    private val tilgangskontroll: TilgangskontrollService,
    private val cacheManager: CacheManager?,
) {
    @Cacheable(DigisosSakCacheConfig.CACHE_NAME, key = "#digisosId")
    suspend fun hentDigisosSak(digisosId: String): DigisosSak =
        withContext(Dispatchers.IO) {
            log.debug("Forsøker å hente digisosSak fra /digisos/api/v1/soknader/$digisosId")

            val digisosSak: DigisosSak =
                fiksWebClient
                    .get()
                    .uri(FiksPaths.PATH_DIGISOSSAK, digisosId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                    .retrieve()
                    .bodyToMono<DigisosSak>()
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        val feilmelding = "Fiks - hentDigisosSak feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode == HttpStatus.NOT_FOUND -> FiksNotFoundException(feilmelding, e)
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }.awaitSingleOrNull()
                    ?: throw BadStateException("digisosSak er null selv om request ikke har kastet exception")

            digisosSak.also { log.debug("Hentet DigisosSak fra Fiks") }
        }

    suspend fun hentAlleDigisosSaker(): List<DigisosSak> =
        withContext(Dispatchers.IO) {
            val digisosSaker: List<DigisosSak> =
                fiksWebClient
                    .get()
                    .uri(FiksPaths.PATH_ALLE_DIGISOSSAKER)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                    .retrieve()
                    .bodyToMono<List<DigisosSak>>()
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        val feilmelding = "Fiks - hentAlleDigisosSaker feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }.awaitSingleOrNull()
                    ?: throw FiksClientException(500, "digisosSak er null selv om request ikke har kastet exception", null)
            val cache = cacheManager?.getCache("digisosSak")
            digisosSaker.onEach {
                tilgangskontroll.verifyDigisosSakIsForCorrectUser(it)
                cache?.put(it.fiksDigisosId, it)
            }
        }

    suspend fun lastOppNyEttersendelse(
        body: MultiValueMap<String, HttpEntity<*>>,
        kommunenummer: String,
        digisosId: String,
        navEksternRefId: String,
    ): ResponseEntity<String> =
        withContext(Dispatchers.IO) {
            fiksWebClient
                .post()
                .uri(FiksPaths.PATH_LAST_OPP_ETTERSENDELSE, kommunenummer, digisosId, navEksternRefId)
                .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .toEntity<String>()
                .onErrorMap(WebClientResponseException::class.java) { e ->
                    if (e.statusCode.value() == 400 && filErAlleredeLastetOpp(e, digisosId)) {
                        val feilmeldingAlleredeFinnes =
                            "Fiks - Opplasting av ettersendelse finnes allerede hos Fiks - ${messageUtenFnr(e)}"
                        log.warn(feilmeldingAlleredeFinnes, e)
                        FiksClientFileExistsException(feilmeldingAlleredeFinnes, e)
                    } else {
                        val feilmelding =
                            "Fiks - Opplasting av ettersendelse til digisosId=$digisosId feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode.value() == 410 -> FiksGoneException(feilmelding, e)
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }
                }.subscribeOn(Schedulers.boundedElastic())
                .block() ?: throw FiksClientException(
                500,
                "responseEntity er null selv om request ikke har kastet exception",
                null,
            )
        }

    private fun filErAlleredeLastetOpp(
        exception: WebClientResponseException,
        digisosId: String,
    ): Boolean =
        toFiksErrorMessageUtenFnr(exception).startsWith("Ettersendelse med tilhørende navEksternRefId ") &&
            toFiksErrorMessageUtenFnr(exception).endsWith(" finnes allerde for oppgitt DigisosId $digisosId")

    @Cacheable(DokumentCacheConfig.CACHE_NAME, key = "#cacheKey")
    suspend fun <T : Serializable> hentDokument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        cacheKey: String,
    ): T =
        withContext(Dispatchers.IO) {
            log.debug("Forsøker å hente dokument fra /digisos/api/v1/soknader/$digisosId/dokumenter/$dokumentlagerId")
            val dokument =
                fiksWebClient
                    .get()
                    .uri(FiksPaths.PATH_DOKUMENT, digisosId, dokumentlagerId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                    .retrieve()
                    .bodyToMono(requestedClass)
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        val feilmelding = "Fiks - hentDokument feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }.awaitSingleOrNull()
                    ?: throw FiksClientException(500, "dokument er null selv om request ikke har kastet exception", null)

            dokument.also { log.debug("Hentet dokument (${requestedClass.simpleName}) fra Fiks, dokumentlagerId=$dokumentlagerId") }
        }

    companion object {
        private val log by logger()
    }
}

class FiksGoneException(
    message: String?,
    e: WebClientResponseException?,
) : RuntimeException(message, e)

class FiksClientFileExistsException(
    message: String?,
    e: WebClientResponseException?,
) : RuntimeException(message, e)

data class VedleggMetadata(
    val filnavn: String?,
    val mimetype: String?,
    val storrelse: Long,
)

fun Any.toHttpEntity(
    name: String,
    filename: String? = null,
    contentType: String = MediaType.APPLICATION_JSON_VALUE,
): HttpEntity<Any> {
    val headerMap = LinkedMultiValueMap<String, String>()
    val builder: ContentDisposition.Builder =
        ContentDisposition
            .builder("form-data")
            .name(name)
    val contentDisposition: ContentDisposition =
        if (filename == null) builder.build() else builder.filename(filename).build()

    headerMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
    headerMap.add(HttpHeaders.CONTENT_TYPE, contentType)
    return HttpEntity(this, HttpHeaders(headerMap))
}
