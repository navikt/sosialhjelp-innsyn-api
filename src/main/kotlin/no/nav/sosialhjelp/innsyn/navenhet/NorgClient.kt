package no.nav.sosialhjelp.innsyn.navenhet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.client.RetryUtils.retryBackoffSpec
import no.nav.sosialhjelp.innsyn.app.exceptions.BadStateException
import no.nav.sosialhjelp.innsyn.app.exceptions.NorgException
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.redis.NAVENHET_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

interface NorgClient {
    suspend fun hentNavEnhet(enhetsnr: String): NavEnhet
}

@Component
class NorgClientImpl(
    private val norgWebClient: WebClient,
    private val redisService: RedisService,
) : NorgClient {
    private val norgRetry =
        retryBackoffSpec()
            .onRetryExhaustedThrow { spec, retrySignal ->
                throw NorgException("Norg - retry har nådd max antall forsøk (=${spec.maxAttempts})", retrySignal.failure())
            }

    override suspend fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return hentFraCache(enhetsnr) ?: hentFraNorg(enhetsnr)
    }

    private suspend fun hentFraNorg(enhetsnr: String): NavEnhet =
        withContext(Dispatchers.IO) {
            log.debug("Forsøker å hente Nav-enhet $enhetsnr fra NORG2")
            val navEnhet: NavEnhet =
                norgWebClient.get()
                    .uri("/enhet/{enhetsnr}", enhetsnr)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
                    .retrieve()
                    .bodyToMono<NavEnhet>()
                    .retryWhen(norgRetry)
                    .onErrorMap(WebClientResponseException::class.java) { e ->
                        log.warn("Noe feilet ved kall mot NORG2 ${e.statusCode}", e)
                        NorgException(e.message, e)
                    }
                    .awaitSingleOrNull()
                    ?: throw BadStateException("Ingen feil, men heller ingen NavEnhet")

            log.info("Hentet Nav-enhet $enhetsnr fra NORG2")

            navEnhet.also { lagreTilCache(enhetsnr, it) }
        }

    private fun hentFraCache(enhetsnr: String): NavEnhet? = redisService.get(cacheKey(enhetsnr), NavEnhet::class.java)

    private fun lagreTilCache(
        enhetsnr: String,
        navEnhet: NavEnhet,
    ) {
        redisService.put(
            cacheKey(enhetsnr),
            objectMapper.writeValueAsBytes(navEnhet),
            NAVENHET_CACHE_TIMETOLIVE_SECONDS,
        )
    }

    private fun cacheKey(enhetsnr: String): String = NAVENHET_CACHE_KEY_PREFIX + enhetsnr

    companion object {
        private val log by logger()

        private const val NAVENHET_CACHE_TIMETOLIVE_SECONDS: Long = 60 * 60 // 1 time
    }
}
