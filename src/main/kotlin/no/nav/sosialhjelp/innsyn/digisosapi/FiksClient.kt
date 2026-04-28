package no.nav.sosialhjelp.innsyn.digisosapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksNotFoundException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.messageUtenFnr
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.utils.toFiksErrorMessageUtenFnr
import no.nav.sosialhjelp.innsyn.valkey.DigisosSakCacheConfig
import no.nav.sosialhjelp.innsyn.valkey.DokumentCacheConfig
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.get
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import java.io.Serializable

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
                }.awaitSingleOrNull()
                ?: throw FiksClientException(
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

    suspend fun hentAlleDokumenter(
        saker: List<DigisosSak>,
        kommuneDeaktivert: Map<String, Boolean>,
    ): Map<String, JsonDigisosSoker> {
        val sakMap = saker.associateBy { it.fiksDigisosId }
        val ids =
            saker
                .filter {
                    it.digisosSoker?.metadata != null && it.digisosSoker?.timestampSistOppdatert != null &&
                        kommuneDeaktivert[it.fiksDigisosId] == false
                }.associate { it.fiksDigisosId to it.digisosSoker?.metadata!! }
        if (ids.isEmpty()) {
            return emptyMap()
        }
        val body =
            AlleDokumenterBody(
                ids.map { (digisosId, dokumentlagerId) ->
                    AlleDokumenterBody.Dokument(
                        digisosId,
                        dokumentlagerId,
                    )
                },
            )

        return withContext(Dispatchers.IO) {
            log.debug("Forsøker å hente ${body.dokumenter.size} dokument fra /digisos/api/v1/soknader/dokumenter")
            fiksWebClient
                .post()
                .uri(FiksPaths.PATH_DOKUMENT_ALLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.MULTIPART_MIXED)
                .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
                .retrieve()
                .onStatus({ !it.is2xxSuccessful }) { clientResponse ->
                    clientResponse.createException().map { e ->
                        val feilmelding = "Fiks - hentAlleDokumenter feilet - ${messageUtenFnr(e)}"
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), feilmelding, e)
                            else -> FiksServerException(e.statusCode.value(), feilmelding, e)
                        }
                    }
                }.multipartBodyDigisosSoker()
                .updateCache(sakMap)
                .mapKeys { (key) ->
                    key.split("_").first()
                }
        }
    }

    companion object {
        private val log by logger()
    }

    private suspend fun Map<String, JsonDigisosSoker>.updateCache(sakMap: Map<String, DigisosSak>): Map<String, JsonDigisosSoker> {
        val cache = cacheManager?.get(DokumentCacheConfig.CACHE_NAME)
        return onEach { (name, digisosSoker) ->
            val (fiksDigisosId, dokumentLagerId) = name.split("_", limit = 2)
            val timestampSistOppdatert = sakMap[fiksDigisosId]?.digisosSoker?.timestampSistOppdatert
            if (timestampSistOppdatert != null) {
                val key = "${dokumentLagerId}_$timestampSistOppdatert"
                cache?.put(key, digisosSoker)
            }
        }
    }
}

private suspend fun WebClient.ResponseSpec.multipartBodyDigisosSoker(): Map<String, JsonDigisosSoker> =
    bodyToFlow<Part>()
        .mapNotNull { part ->
            // Kommer på format $fiksDigisosId_$dokumentLagerId
            val name = part.headers().contentDisposition.name
            val content =
                DataBufferUtils.join(part.content()).awaitSingleOrNull()?.let { dataBuffer ->
                    try {
                        val bytes = ByteArray(dataBuffer.readableByteCount()).also { dataBuffer.read(it) }
                        sosialhjelpJsonMapper.readValue(bytes, JsonDigisosSoker::class.java)
                    } finally {
                        DataBufferUtils.release(dataBuffer)
                    }
                }
            if (name != null && content != null) {
                name to content
            } else {
                null
            }
        }.toList()
        .toMap()

class FiksGoneException(
    message: String?,
    e: WebClientResponseException?,
) : RuntimeException(message, e)

class FiksClientFileExistsException(
    message: String?,
    e: WebClientResponseException?,
) : RuntimeException(message, e)
