package no.nav.sosialhjelp.innsyn.digisosapi.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClientImpl
import no.nav.sosialhjelp.innsyn.digisosapi.VedleggMetadata
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.digisosapi.test.dto.FilOpplastingResponse
import no.nav.sosialhjelp.innsyn.digisosapi.toHttpEntity
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

/**
 * Brukes kun i dev eller ved lokal testing mot fiks-test
 */
@Profile("!prodgcp")
@Component
class DigisosApiTestClientImpl(
    private val fiksWebClient: WebClient,
    private val digisosApiTestWebClient: WebClient,
    private val texasClient: TexasClient,
    private val fiksClientImpl: FiksClientImpl,
) : DigisosApiTestClient {
    private val testbrukerNatalie = System.getenv("TESTBRUKER_NATALIE") ?: "11111111111"

    override suspend fun oppdaterDigisosSak(
        fiksDigisosId: String?,
        digisosApiWrapper: DigisosApiWrapper,
    ): String? =
        withContext(Dispatchers.IO) {
            var id = fiksDigisosId
            if (fiksDigisosId == null || fiksDigisosId == "001" || fiksDigisosId == "002" || fiksDigisosId == "003") {
                id = opprettDigisosSak()
                log.info("Laget ny digisossak: $id")
            }

            digisosApiTestWebClient
                .post()
                .uri("/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$id")
                .header(AUTHORIZATION, BEARER + texasClient.getMaskinportenToken())
                .body(BodyInserters.fromValue(objectMapper.writeValueAsString(digisosApiWrapper)))
                .retrieve()
                .bodyToMono<String>()
                .onErrorMap(WebClientResponseException::class.java) { e ->
                    log.warn("Fiks - oppdaterDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
                    when {
                        e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), e.message, e)
                        else -> FiksServerException(e.statusCode.value(), e.message, e)
                    }
                }.awaitSingleOrNull()
                .also { log.info("Postet DigisosSak til Fiks og fikk response: $it") }
                ?: id
        }

    // Brukes for å laste opp Pdf-er fra test-fagsystem i q-miljø
    override suspend fun lastOppNyeFilerTilFiks(
        files: List<FilForOpplasting>,
        soknadId: String,
    ): List<String> {
        val body = LinkedMultiValueMap<String, Any>()
        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add(
                "vedleggSpesifikasjon:$fileId",
                fiksClientImpl.serialiser(vedleggMetadata).toHttpEntity("vedleggSpesifikasjon:$fileId"),
            )
            body.add("dokument:$fileId", file.toHttpEntity("dokument:$fileId"))
        }

        val opplastingResponseList =
            withContext(Dispatchers.IO) {
                digisosApiTestWebClient
                    .post()
                    .uri("/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$soknadId/filer")
                    .header(AUTHORIZATION, BEARER + texasClient.getMaskinportenToken())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono<List<FilOpplastingResponse>>()
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        log.warn("Fiks - Opplasting av filer feilet - ${e.statusCode} ${e.statusText}", e)
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), e.message, e)
                            else -> FiksServerException(e.statusCode.value(), e.message, e)
                        }
                    }.awaitSingleOrNull()
                    ?: throw BadStateException("Ingen feil, men heller ingen opplastingResponseList")
            }
        log.info("Filer sendt til Fiks")
        return opplastingResponseList.map { it.dokumentlagerDokumentId }
    }

    override suspend fun hentInnsynsfil(
        fiksDigisosId: String,
        token: String,
    ): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val soknad =
                    fiksWebClient
                        .get()
                        .uri("/digisos/api/v1/soknader/$fiksDigisosId")
                        .accept(MediaType.APPLICATION_JSON)
                        .header(AUTHORIZATION, token)
                        .retrieve()
                        .bodyToMono(DigisosSak::class.java)
                        .onErrorMap(WebClientResponseException::class.java) { e ->
                            log.warn("Fiks - Nedlasting av søknad feilet - ${e.statusCode} ${e.statusText}", e)
                            when {
                                e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), e.message, e)
                                else -> FiksServerException(e.statusCode.value(), e.message, e)
                            }
                        }.awaitSingleOrNull()
                        ?: throw BadStateException("Ingen feil, men heller ingen soknad")
                val digisosSoker = soknad.digisosSoker ?: throw BadStateException("Soknad mangler digisosSoker")
                fiksWebClient
                    .get()
                    .uri("/digisos/api/v1/soknader/$fiksDigisosId/dokumenter/${digisosSoker.metadata}")
                    .accept(MediaType.APPLICATION_JSON)
                    .header(AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        log.warn("Fiks - Nedlasting av innsynsfil feilet - ${e.statusCode} ${e.statusText}", e)
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), e.message, e)
                            else -> FiksServerException(e.statusCode.value(), e.message, e)
                        }
                    }.awaitSingleOrNull()
            }.getOrNull()
        }

    suspend fun opprettDigisosSak(): String? =
        withContext(Dispatchers.IO) {
            val response =
                digisosApiTestWebClient
                    .post()
                    .uri("/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/ny?sokerFnr=$testbrukerNatalie")
                    .header(AUTHORIZATION, BEARER + texasClient.getMaskinportenToken())
                    .body(BodyInserters.fromValue(""))
                    .retrieve()
                    .bodyToMono<String>()
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        log.warn("Fiks - opprettDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
                        when {
                            e.statusCode.is4xxClientError -> FiksClientException(e.statusCode.value(), e.message, e)
                            else -> FiksServerException(e.statusCode.value(), e.message, e)
                        }
                    }.awaitSingleOrNull()
            log.info("Opprettet sak hos Fiks. Digisosid: $response")
            response?.replace("\"", "")
        }

    companion object {
        private val log by logger()
    }
}
