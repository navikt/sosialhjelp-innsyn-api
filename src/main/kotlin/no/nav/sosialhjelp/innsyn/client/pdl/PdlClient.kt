package no.nav.sosialhjelp.innsyn.client.pdl

import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.innsyn.common.PdlException
import no.nav.sosialhjelp.innsyn.redis.ADRESSEBESKYTTELSE_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.kotlin.utils.retry
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Optional
import java.util.stream.Collectors

interface PdlClient {
    fun hentPerson(ident: String, token: String): PdlHentPerson?
    fun ping()
}

@Profile("!local")
@Component
class PdlClientImpl(
    private val pdlWebClient: WebClient,
    private val pdlHeadersService: PdlHeadersService,
    private val redisService: RedisService,
) : PdlClient {

    override fun hentPerson(ident: String, token: String): PdlHentPerson? {
        return hentFraCache(ident) ?: hentFraPdl(ident, token)
    }

    private fun hentFraCache(ident: String): PdlHentPerson? =
        redisService.get(cacheKey(ident), PdlHentPerson::class.java) as? PdlHentPerson

    private fun hentFraPdl(ident: String, token: String): PdlHentPerson? {
        val query = getQuery()
        try {
            val pdlPersonResponse = runBlocking {
                retry(
                    attempts = RETRY_ATTEMPTS,
                    initialDelay = INITIAL_DELAY,
                    maxDelay = MAX_DELAY,
                    retryableExceptions = arrayOf(WebClientResponseException::class)
                ) {
                    pdlWebClient.post()
                        .headers { it.addAll(headers(ident, token)) }
                        .bodyValue(PdlRequest(query, Variables(ident)))
                        .retrieve()
                        .awaitBody<PdlPersonResponse>()
                }
            }

            checkForPdlApiErrors(pdlPersonResponse)

            return pdlPersonResponse.data
                .also { it?.let { lagreTilCache(ident, it) } }
        } catch (e: WebClientResponseException) {
            log.error("PDL - noe feilet, status=${e.rawStatusCode} ${e.statusText}", e)
            throw PdlException(e.message!!)
        }
    }

    override fun ping() {
        pdlWebClient.options()
            .retrieve()
            .bodyToMono<String>()
            .doOnError { e ->
                log.error("PDL - ping feilet", e)
            }
    }

    private fun getQuery(): String =
        this.javaClass.getResource("/pdl/hentPerson.graphql")?.readText()?.replace("[\n\r]", "")
            ?: throw RuntimeException("Feil ved lesing av graphql-spørring fra fil")

    private fun headers(ident: String, token: String): HttpHeaders {
        return pdlHeadersService.getHeaders(ident, token)
    }

    private fun checkForPdlApiErrors(response: PdlPersonResponse?) {
        Optional.ofNullable(response)
            .map(PdlPersonResponse::errors)
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

    companion object {
        private val log by logger()

        private const val RETRY_ATTEMPTS = 5
        private const val INITIAL_DELAY = 100L
        private const val MAX_DELAY = 2000L
    }
}
