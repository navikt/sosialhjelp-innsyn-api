package no.nav.sosialhjelp.innsyn.client.norg

import no.nav.sosialhjelp.innsyn.common.NorgException
import no.nav.sosialhjelp.innsyn.domain.NavEnhet
import no.nav.sosialhjelp.innsyn.redis.NAVENHET_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

interface NorgClient {
    fun hentNavEnhet(enhetsnr: String): NavEnhet
    fun ping()
}

@Profile("!local")
@Component
class NorgClientImpl(
    private val norgWebClient: WebClient,
    private val redisService: RedisService,
) : NorgClient {

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return hentFraCache(enhetsnr) ?: hentFraNorg(enhetsnr)
    }

    private fun hentFraNorg(enhetsnr: String): NavEnhet {
        log.debug("Forsøker å hente NAV-enhet $enhetsnr fra NORG2")

        val navEnhet: NavEnhet? = norgWebClient.get()
            .uri("/enhet/{enhetsnr}", enhetsnr)
            .headers { it.addAll(headers()) }
            .retrieve()
            .bodyToMono<NavEnhet>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Noe feilet ved kall mot NORG2 ${e.statusCode}", e)
                NorgException(e.message, e)
            }
            .block()

        log.info("Hentet NAV-enhet $enhetsnr fra NORG2")

        return navEnhet!!
            .also { lagreTilCache(enhetsnr, it) }
    }

    private fun hentFraCache(enhetsnr: String): NavEnhet? =
        redisService.get(cacheKey(enhetsnr), NavEnhet::class.java) as NavEnhet?

    // samme kall som selftest i soknad-api
    override fun ping() {
        norgWebClient.get()
            .uri("/kodeverk/EnhetstyperNorg")
            .headers { it.addAll(headers()) }
            .retrieve()
            .bodyToMono<String>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Ping - feilet mot NORG2 ${e.statusCode}", e)
                NorgException(e.message, e)
            }
            .block()
    }

    private fun headers(): HttpHeaders {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.APPLICATION_JSON)
        headers.set(IntegrationUtils.HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
        headers.set(IntegrationUtils.HEADER_NAV_APIKEY, System.getenv(NORG2_APIKEY))
        return headers
    }

    private fun lagreTilCache(enhetsnr: String, navEnhet: NavEnhet) {
        redisService.put(
            cacheKey(enhetsnr),
            objectMapper.writeValueAsBytes(navEnhet),
            NAVENHET_CACHE_TIMETOLIVE_SECONDS
        )
    }

    private fun cacheKey(enhetsnr: String): String = NAVENHET_CACHE_KEY_PREFIX + enhetsnr

    companion object {
        private val log by logger()

        private const val NORG2_APIKEY = "SOSIALHJELP_INNSYN_API_NORG2_APIKEY_PASSWORD"
        private const val NAVENHET_CACHE_TIMETOLIVE_SECONDS: Long = 60 * 60 // 1 time
    }
}

@Profile("local")
@Component
class NorgClientLocal : NorgClient {

    private val innsynMap = mutableMapOf<String, NavEnhet>()

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return innsynMap.getOrElse(
            enhetsnr
        ) {
            val default = NavEnhet(
                enhetId = 100000367,
                navn = "NAV Longyearbyen",
                enhetNr = enhetsnr,
                antallRessurser = 20,
                status = "AKTIV",
                aktiveringsdato = "1982-04-21",
                nedleggelsesdato = "null"
            )
            innsynMap[enhetsnr] = default
            default
        }
    }

    override fun ping() {
        // no-op
    }
}
