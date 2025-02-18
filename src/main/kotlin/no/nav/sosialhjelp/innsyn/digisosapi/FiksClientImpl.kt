package no.nav.sosialhjelp.innsyn.digisosapi

import com.fasterxml.jackson.core.JsonProcessingException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.client.RetryUtils.retryBackoffSpec
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.SECURE
import no.nav.sosialhjelp.innsyn.utils.lagNavEksternRefId
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.messageUtenFnr
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.utils.toFiksErrorMessageUtenFnr
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import java.io.Serializable

@Component
class FiksClientImpl(
    private val fiksWebClient: WebClient,
    private val tilgangskontroll: TilgangskontrollService,
    @Value("\${retry_fiks_max_attempts}") private val retryMaxAttempts: Long,
    @Value("\${retry_fiks_initial_delay}") private val retryInitialDelay: Long,
    meterRegistry: MeterRegistry,
) : FiksClient {
    private val opplastingsteller: Counter = meterRegistry.counter("filopplasting")

    private val fiksRetry =
        retryBackoffSpec(maxAttempts = retryMaxAttempts, initialWaitIntervalMillis = retryInitialDelay)
            .onRetryExhaustedThrow { _, retrySignal ->
                throw FiksServerException(
                    status = SERVICE_UNAVAILABLE.value(),
                    message = "Fiks - retry har nådd max antall forsøk (=$retryMaxAttempts)",
                    cause = retrySignal.failure(),
                )
            }

    @Cacheable("digisosSak", key = "#digisosId")
    override suspend fun hentDigisosSak(
        digisosId: String,
        token: String,
    ): DigisosSak {
        return hentDigisosSakFraFiks(digisosId, token).also { tilgangskontroll.verifyDigisosSakIsForCorrectUser(it) }
    }

    @Cacheable("digisosSak", key = "#digisosId")
    override suspend fun hentDigisosSakMedFnr(
        digisosId: String,
        token: String,
        fnr: String,
    ): DigisosSak {
        val sak = hentDigisosSakFraFiks(digisosId, token)

        // TODO henting av fnr og sammeligning benyttes til søk i feilsituasjon. Fjernes når feilsøking er ferdig.
        val fnr2 = TokenUtils.getUserIdFromToken()

        if (fnr2 != fnr) {
            log.error("Fødselsnr i kontekst har blitt endret - FiksClient.hentDigisosSak")
        }
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(sak)
        return sak
    }

    private suspend fun hentDigisosSakFraFiks(
        digisosId: String,
        token: String,
    ): DigisosSak =
        withContext(Dispatchers.IO) {
            log.debug("Forsøker å hente digisosSak fra /digisos/api/v1/soknader/$digisosId")

            val digisosSak: DigisosSak =
                fiksWebClient.get()
                    .uri(FiksPaths.PATH_DIGISOSSAK, digisosId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono<DigisosSak>()
                    .retryWhen(fiksRetry)
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        val feilmelding = "Fiks - hentDigisosSak feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode == HttpStatus.NOT_FOUND -> FiksNotFoundException(feilmelding, e)
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }
                    .awaitSingleOrNull()
                    ?: throw BadStateException("digisosSak er null selv om request ikke har kastet exception")

            digisosSak.also { log.debug("Hentet DigisosSak fra Fiks") }
        }

    @Cacheable("dokument", key = "#cacheKey")
    override suspend fun <T : Serializable> hentDokument(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        token: String,
        cacheKey: String,
    ): T {
        return hentDokumentFraFiks(digisosId, dokumentlagerId, requestedClass, token)
    }

    private suspend fun <T : Any> hentDokumentFraFiks(
        digisosId: String,
        dokumentlagerId: String,
        requestedClass: Class<out T>,
        token: String,
    ): T =
        withContext(Dispatchers.IO) {
            log.info("Forsøker å hente dokument fra /digisos/api/v1/soknader/$digisosId/dokumenter/$dokumentlagerId")
            val dokument =
                fiksWebClient.get()
                    .uri(FiksPaths.PATH_DOKUMENT, digisosId, dokumentlagerId)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono(requestedClass)
                    .retryWhen(fiksRetry)
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        val feilmelding = "Fiks - hentDokument feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }
                    .awaitSingleOrNull()
                    ?: throw FiksClientException(500, "dokument er null selv om request ikke har kastet exception", null)

            dokument.also { log.info("Hentet dokument (${requestedClass.simpleName}) fra Fiks, dokumentlagerId=$dokumentlagerId") }
        }

    override suspend fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        log.info(SECURE, "Vi kaller /soknader/soknader hos fiks for bruker")
        return withContext(Dispatchers.IO) {
            val digisosSaker: List<DigisosSak> =
                fiksWebClient.get()
                    .uri(FiksPaths.PATH_ALLE_DIGISOSSAKER)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono<List<DigisosSak>>()
                    .retryWhen(fiksRetry)
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        val feilmelding = "Fiks - hentAlleDigisosSaker feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }
                    .awaitSingleOrNull()
                    ?: throw FiksClientException(500, "digisosSak er null selv om request ikke har kastet exception", null)

            log.info(SECURE, "Fant ${digisosSaker.size} digisosSaker hos Fiks")
            digisosSaker.onEach { tilgangskontroll.verifyDigisosSakIsForCorrectUser(it) }
        }
    }

    override suspend fun lastOppNyEttersendelse(
        files: List<FilForOpplasting>,
        vedleggJson: JsonVedleggSpesifikasjon,
        digisosId: String,
        token: String,
    ) {
        log.info(
            "Starter sending til FIKS for ettersendelse med ${files.size} filer (inkludert ettersendelse.pdf)." +
                " Validering, filnavn-endring, generering av ettersendelse.pdf og kryptering er OK.",
        )

        val body = createBodyForUpload(vedleggJson, files)

        val digisosSak = hentDigisosSakFraFiks(digisosId, token)
        tilgangskontroll.verifyDigisosSakIsForCorrectUser(digisosSak)
        val kommunenummer = digisosSak.kommunenummer
        if (kommunenummer == "1507") {
            error("Kan ikke laste opp vedlegg på søknad fra Ålesund kommune")
        }
        val navEksternRefId = lagNavEksternRefId(digisosSak)

        if (erPapirsoknad(digisosSak)) {
            log.info("Kommune ${digisosSak.kommunenummer} har innsyn i papirsøknader.")
        }

        val responseEntity =
            withContext(Dispatchers.IO) {
                fiksWebClient.post()
                    .uri(FiksPaths.PATH_LAST_OPP_ETTERSENDELSE, kommunenummer, digisosId, navEksternRefId)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
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
                                e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                                else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                            }
                        }
                    }
                    .awaitSingleOrNull() ?: throw FiksClientException(
                    500,
                    "responseEntity er null selv om request ikke har kastet exception",
                    null,
                )
            }

        opplastingsteller.increment()
        log.info(
            "Sendte ettersendelse til kommune $kommunenummer i Fiks, " +
                "fikk navEksternRefId $navEksternRefId (statusCode: ${responseEntity.statusCode})",
        )
    }

    private fun erPapirsoknad(digisosSak: DigisosSak): Boolean {
        return digisosSak.ettersendtInfoNAV?.ettersendelser?.isEmpty() != false && digisosSak.originalSoknadNAV == null
    }

    private fun filErAlleredeLastetOpp(
        exception: WebClientResponseException,
        digisosId: String,
    ): Boolean =
        toFiksErrorMessageUtenFnr(exception).startsWith("Ettersendelse med tilhørende navEksternRefId ") &&
            toFiksErrorMessageUtenFnr(exception).endsWith(" finnes allerde for oppgitt DigisosId $digisosId")

    fun createBodyForUpload(
        vedleggJson: JsonVedleggSpesifikasjon,
        files: List<FilForOpplasting>,
    ): LinkedMultiValueMap<String, Any> {
        val body = LinkedMultiValueMap<String, Any>()
        body.add("vedlegg.json", serialiser(vedleggJson).toHttpEntity("vedlegg.json"))

        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add(
                "vedleggSpesifikasjon:$fileId",
                serialiser(vedleggMetadata).toHttpEntity("vedleggSpesifikasjon:$fileId"),
            )
            body.add("dokument:$fileId", file.toHttpEntity("dokument:$fileId"))
        }
        return body
    }

    fun serialiser(metadata: Any): String {
        try {
            return objectMapper.writeValueAsString(metadata)
        } catch (e: JsonProcessingException) {
            throw RuntimeException("Feil under serialisering av metadata", e)
        }
    }

    companion object {
        private val log by logger()
    }
}

class FiksClientFileExistsException(message: String?, e: WebClientResponseException?) : RuntimeException(message, e)

data class VedleggMetadata(
    val filnavn: String?,
    val mimetype: String?,
    val storrelse: Long,
)

private fun Any.toHttpEntity(
    name: String,
    filename: String?,
    contentType: String,
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
    return HttpEntity(this, headerMap)
}

fun FilForOpplasting.toHttpEntity(name: String): HttpEntity<Any> {
    return InputStreamResource(this.fil).toHttpEntity(name, this.filnavn, "application/octet-stream")
}

fun String.toHttpEntity(name: String): HttpEntity<Any> {
    return this.toHttpEntity(name, null, "text/plain;charset=UTF-8")
}
