package no.nav.sosialhjelp.innsyn.client.digisosapi

import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClientImpl
import no.nav.sosialhjelp.innsyn.client.fiks.VedleggMetadata
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.DigisosApiWrapper
import no.nav.sosialhjelp.innsyn.service.idporten.IdPortenService
import no.nav.sosialhjelp.innsyn.service.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.utils.typeRef
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Collections

/**
 * Brukes kun i dev-sbs eller ved lokal testing mot fiks-test
 */
@Profile("!(prod-sbs|mock)")
@Component
class DigisosApiClientImpl(
    private val clientProperties: ClientProperties,
    private val fiksWebClient: WebClient,
    private val idPortenService: IdPortenService,
    private val fiksClientImpl: FiksClientImpl,
) : DigisosApiClient {

    private val testbrukerNatalie = System.getenv("TESTBRUKER_NATALIE") ?: "11111111111"

    override fun oppdaterDigisosSak(fiksDigisosId: String?, digisosApiWrapper: DigisosApiWrapper): String? {
        var id = fiksDigisosId
        if (fiksDigisosId == null || fiksDigisosId == "001" || fiksDigisosId == "002" || fiksDigisosId == "003") {
            id = opprettDigisosSak()
            log.info("Laget ny digisossak: $id")
        }

        return fiksWebClient.post()
            .uri("/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$id")
            .headers { it.addAll(headers()) }
            .body(BodyInserters.fromValue(objectMapper.writeValueAsString(digisosApiWrapper)))
            .retrieve()
            .bodyToMono<String>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Fiks - oppdaterDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
                when {
                    e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                    else -> FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .block()
            .also { log.info("Postet DigisosSak til Fiks") }
    }

    // Brukes for å laste opp Pdf-er fra test-fagsystem i q-miljø
    override fun lastOppNyeFilerTilFiks(files: List<FilForOpplasting>, soknadId: String): List<String> {
        val body = LinkedMultiValueMap<String, Any>()
        files.forEachIndexed { fileId, file ->
            val vedleggMetadata = VedleggMetadata(file.filnavn, file.mimetype, file.storrelse)
            body.add("vedleggSpesifikasjon:$fileId", fiksClientImpl.createHttpEntityOfString(fiksClientImpl.serialiser(vedleggMetadata), "vedleggSpesifikasjon:$fileId"))
            body.add("dokument:$fileId", fiksClientImpl.createHttpEntityOfFile(file, "dokument:$fileId"))
        }

        val opplastingResponseList = fiksWebClient.post()
            .uri("/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/$soknadId/filer")
            .headers { it.addAll(headers()) }
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(body))
            .retrieve()
            .bodyToMono(typeRef<List<FilOpplastingResponse>>())
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Fiks - Opplasting av filer feilet - ${e.statusCode} ${e.statusText}", e)
                when {
                    e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                    else -> FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .block()
        log.info("Filer sendt til Fiks")
        return opplastingResponseList!!.map { it.dokumentlagerDokumentId }
    }

    override fun hentInnsynsfil(fiksDigisosId: String, token: String): String? {
        try {
            val soknad = fiksWebClient.get()
                .uri("/digisos/api/v1/soknader/$fiksDigisosId")
                .headers { it.addAll(IntegrationUtils.fiksHeaders(clientProperties, token)) }
                .retrieve()
                .bodyToMono(DigisosSak::class.java)
                .onErrorMap(WebClientResponseException::class.java) { e ->
                    log.warn("Fiks - Nedlasting av søknad feilet - ${e.statusCode} ${e.statusText}", e)
                    when {
                        e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                        else -> FiksServerException(e.rawStatusCode, e.message, e)
                    }
                }
                .block()
                ?: return null
            return fiksWebClient.get()
                .uri("/digisos/api/v1/soknader/$fiksDigisosId/dokumenter/${soknad.digisosSoker!!.metadata}")
                .headers { it.addAll(IntegrationUtils.fiksHeaders(clientProperties, token)) }
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorMap(WebClientResponseException::class.java) { e ->
                    log.warn("Fiks - Nedlasting av innsynsfil feilet - ${e.statusCode} ${e.statusText}", e)
                    when {
                        e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                        else -> FiksServerException(e.rawStatusCode, e.message, e)
                    }
                }
                .block()
        } catch (e: RuntimeException) {
            return null
        }
    }

    fun opprettDigisosSak(): String? {
        val response = fiksWebClient.post()
            .uri("/digisos/api/v1/11415cd1-e26d-499a-8421-751457dfcbd5/ny?sokerFnr=$testbrukerNatalie")
            .headers { it.addAll(headers()) }
            .body(BodyInserters.fromValue(""))
            .retrieve()
            .bodyToMono<String>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Fiks - opprettDigisosSak feilet - ${e.statusCode} ${e.statusText}", e)
                when {
                    e.statusCode.is4xxClientError -> FiksClientException(e.rawStatusCode, e.message, e)
                    else -> FiksServerException(e.rawStatusCode, e.message, e)
                }
            }
            .block()
        log.info("Opprettet sak hos Fiks. Digisosid: $response")
        return response?.replace("\"", "")
    }

    private fun headers(): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonIdKommune)
        headers.set(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonPassordKommune)
        headers.set(AUTHORIZATION, BEARER + idPortenService.getToken().token)
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
    val storrelse: Long,
)
