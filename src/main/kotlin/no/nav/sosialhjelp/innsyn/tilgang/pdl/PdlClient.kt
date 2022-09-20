package no.nav.sosialhjelp.innsyn.tilgang.pdl

import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.RetryUtils
import no.nav.sosialhjelp.innsyn.app.exceptions.PdlException
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.app.tokendings.TokendingsService
import no.nav.sosialhjelp.innsyn.redis.ADRESSEBESKYTTELSE_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.PDL_IDENTER_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_TEMA
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.TEMA_KOM
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Optional
import java.util.stream.Collectors

interface PdlClient {
    fun hentPerson(ident: String, token: String): PdlHentPerson?
    fun hentIdenter(ident: String, token: String): List<String>?
    fun ping()
}

@Profile("!local")
@Component
class PdlClientImpl(
    private val pdlWebClient: WebClient,
    private val clientProperties: ClientProperties,
    private val tokendingsService: TokendingsService,
    private val redisService: RedisService,
) : PdlClient {

    private val pdlRetry = RetryUtils.retryBackoffSpec({ it is WebClientResponseException })
        .onRetryExhaustedThrow { spec, retrySignal ->
            throw PdlException("Pdl - retry har nådd max antall forsøk (=${spec.maxAttempts})", retrySignal.failure())
        }

    override fun hentPerson(ident: String, token: String): PdlHentPerson? {
        return hentFraCache(ident) ?: hentFraPdl(ident, token)
    }

    override fun hentIdenter(ident: String, token: String): List<String> {
        redisService.get(PDL_IDENTER_CACHE_KEY_PREFIX + ident, PdlIdenter::class.java)
            ?.let { pdlIdenter -> return (pdlIdenter as PdlIdenter).identer.map { it.ident } }
        return hentIdenterFraPdl(ident, token)?.identer?.map { it.ident } ?: emptyList()
    }

    private fun hentFraCache(ident: String): PdlHentPerson? =
        redisService.get(cacheKey(ident), PdlHentPerson::class.java) as? PdlHentPerson

    private fun hentFraPdl(ident: String, token: String): PdlHentPerson? {
        val query = getHentPersonResource().replace("[\n\r]", "")
        try {
            val pdlPersonResponse = pdlWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
                .header(HEADER_TEMA, TEMA_KOM)
                .header(AUTHORIZATION, BEARER + tokenXtoken(ident, token))
                .bodyValue(PdlRequest(query, Variables(ident)))
                .retrieve()
                .bodyToMono<PdlPersonResponse>()
                .retryWhen(pdlRetry)
                .block()

            checkForPdlApiErrors(pdlPersonResponse)

            return pdlPersonResponse?.data
                .also { it?.let { lagreTilCache(ident, it) } }
        } catch (e: WebClientResponseException) {
            log.error("PDL - noe feilet, status=${e.rawStatusCode} ${e.statusText}", e)
            throw PdlException(e.message ?: "Ukjent PdlException")
        }
    }

    private fun hentIdenterFraPdl(ident: String, token: String): PdlIdenter? {
        val query = getHentIdenterResource().replace("[\n\r]", "")
        try {
            val pdlIdenterResponse = pdlWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
                .header(HEADER_TEMA, TEMA_KOM)
                .header(AUTHORIZATION, BEARER + tokenXtoken(ident, token))
                .bodyValue(PdlRequest(query, Variables(ident)))
                .retrieve()
                .bodyToMono<PdlIdenterResponse>()
                .retryWhen(pdlRetry)
                .block()

            checkForPdlApiErrors(pdlIdenterResponse)

            return pdlIdenterResponse?.data?.hentIdenter
                .also { it?.let { lagreIdenterTilCache(ident, it) } }
        } catch (e: WebClientResponseException) {
            log.error("PDL - noe feilet, status=${e.rawStatusCode} ${e.statusText}", e)
            throw PdlException(e.message ?: "Ukjent PdlException")
        }
    }

    private fun tokenXtoken(ident: String, token: String) = runBlocking {
        tokendingsService.exchangeToken(ident, token, clientProperties.pdlAudience)
    }

    override fun ping() {
        pdlWebClient.options()
            .retrieve()
            .bodyToMono<String>()
            .doOnError { e ->
                log.error("PDL - ping feilet", e)
            }
            .subscribe()
    }

    private fun getHentPersonResource() = getResourceAsString("/pdl/hentPerson.graphql")
    private fun getHentIdenterResource() = getResourceAsString("/pdl/hentIdenter.graphql")
    private fun getResourceAsString(path: String) =
        this::class.java.getResource(path)?.readText()
            ?: throw RuntimeException("Klarte ikke å hente PDL query resurs: $path")

    private fun checkForPdlApiErrors(response: PdlResponse?) {
        Optional.ofNullable(response)
            .map(PdlResponse::errors)
            .ifPresent { handleErrors(it) }
    }

    private fun handleErrors(errors: List<PdlError>) {
        val errorList = errors.stream()
            .map { it.message + "(feilkode: " + it.extensions.code + ")" }
            .collect(Collectors.toList())
        throw PdlException(errorMessage(errorList))
    }

    private fun errorMessage(errors: List<String>): String =
        "Error i respons fra pdl-api: ${errors.joinToString { it }}"

    private fun cacheKey(ident: String): String = ADRESSEBESKYTTELSE_CACHE_KEY_PREFIX + ident

    private fun lagreTilCache(ident: String, pdlHentPerson: PdlHentPerson) =
        redisService.put(cacheKey(ident), objectMapper.writeValueAsBytes(pdlHentPerson))

    private fun lagreIdenterTilCache(ident: String, pdlIdenter: PdlIdenter) =
        redisService.put(PDL_IDENTER_CACHE_KEY_PREFIX + ident, objectMapper.writeValueAsBytes(pdlIdenter))

    companion object {
        private val log by logger()
    }
}
