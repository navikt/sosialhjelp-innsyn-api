package no.nav.sosialhjelp.innsyn.tilgang.pdl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
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
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

interface PdlClient {
    suspend fun hentPerson(
        ident: String,
        token: String,
    ): PdlHentPerson?

    suspend fun hentIdenter(
        ident: String,
        token: String,
    ): List<String>?

    fun ping()
}

@Component
class PdlClientImpl(
    private val pdlWebClient: WebClient,
    private val clientProperties: ClientProperties,
    private val tokendingsService: TokendingsService,
    private val redisService: RedisService,
) : PdlClient {
    private val pdlRetry =
        RetryUtils.retryBackoffSpec({ it is WebClientResponseException })
            .onRetryExhaustedThrow { spec, retrySignal ->
                throw PdlException("Pdl - retry har nådd max antall forsøk (=${spec.maxAttempts})", retrySignal.failure())
            }

    override suspend fun hentPerson(
        ident: String,
        token: String,
    ): PdlHentPerson? {
        return hentFraCache(ident) ?: hentFraPdl(ident, token)
    }

    override suspend fun hentIdenter(
        ident: String,
        token: String,
    ): List<String> {
        redisService.get(PDL_IDENTER_CACHE_KEY_PREFIX + ident, PdlIdenter::class.java)
            ?.let { pdlIdenter -> return pdlIdenter.identer.map { it.ident } }
        return hentIdenterFraPdl(ident, token)?.identer?.map { it.ident } ?: emptyList()
    }

    private fun hentFraCache(ident: String): PdlHentPerson? = redisService.get(cacheKey(ident), PdlHentPerson::class.java)

    private suspend fun hentFraPdl(
        ident: String,
        token: String,
    ): PdlHentPerson? =
        withContext(Dispatchers.IO) {
            val query = getHentPersonResource().replace("[\n\r]", "")
            try {
                val pdlPersonResponse =
                    pdlWebClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
                        .header(AUTHORIZATION, BEARER + tokenXtoken(ident, token))
                        .bodyValue(PdlRequest(query, Variables(ident)))
                        .retrieve()
                        .bodyToMono<PdlPersonResponse>()
                        .retryWhen(pdlRetry)
                        .awaitSingleOrNull()

                checkForPdlApiErrors(pdlPersonResponse)

                pdlPersonResponse?.data
                    .also { it?.let { lagreTilCache(ident, it) } }
            } catch (e: WebClientResponseException) {
                log.error("PDL - noe feilet, status=${e.statusCode} ${e.statusText}", e)
                throw PdlException(e.message ?: "Ukjent PdlException")
            }
        }

    private suspend fun hentIdenterFraPdl(
        ident: String,
        token: String,
    ): PdlIdenter? {
        val query = getHentIdenterResource().replace("[\n\r]", "")
        try {
            val pdlIdenterResponse =
                pdlWebClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
                    .header(AUTHORIZATION, BEARER + tokenXtoken(ident, token))
                    .bodyValue(PdlRequest(query, Variables(ident)))
                    .retrieve()
                    .bodyToMono<PdlIdenterResponse>()
                    .retryWhen(pdlRetry)
                    .awaitSingleOrNull()

            checkForPdlApiErrors(pdlIdenterResponse)

            return pdlIdenterResponse?.data?.hentIdenter
                .also { it?.let { lagreIdenterTilCache(ident, it) } }
        } catch (e: WebClientResponseException) {
            log.error("PDL - noe feilet, status=${e.statusCode} ${e.statusText}", e)
            throw PdlException(e.message ?: "Ukjent PdlException")
        }
    }

    private suspend fun tokenXtoken(
        ident: String,
        token: String,
    ) = tokendingsService.exchangeToken(ident, token, clientProperties.pdlAudience)

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
        response?.errors?.let { handleErrors(it) }
    }

    private fun handleErrors(errors: List<PdlError>) {
        val errorString =
            errors
                .map { it.message + "(feilkode: " + it.extensions.code + ")" }
                .joinToString(prefix = "Error i respons fra pdl-api: ") { it }
        throw PdlException(errorString)
    }

    private fun cacheKey(ident: String): String = ADRESSEBESKYTTELSE_CACHE_KEY_PREFIX + ident

    private fun lagreTilCache(
        ident: String,
        pdlHentPerson: PdlHentPerson,
    ) = redisService.put(cacheKey(ident), objectMapper.writeValueAsBytes(pdlHentPerson))

    private fun lagreIdenterTilCache(
        ident: String,
        pdlIdenter: PdlIdenter,
    ) = redisService.put(PDL_IDENTER_CACHE_KEY_PREFIX + ident, objectMapper.writeValueAsBytes(pdlIdenter))

    companion object {
        private val log by logger()
    }
}
