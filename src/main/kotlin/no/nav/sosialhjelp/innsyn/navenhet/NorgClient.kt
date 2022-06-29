package no.nav.sosialhjelp.innsyn.navenhet

import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.innsyn.client.tokendings.TokendingsService
import no.nav.sosialhjelp.innsyn.common.BadStateException
import no.nav.sosialhjelp.innsyn.common.NorgException
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils.getToken
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.redis.NAVENHET_CACHE_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

interface NorgClient {
    fun hentNavEnhet(enhetsnr: String): NavEnhet
}

@Profile("!local")
@Component
class NorgClientImpl(
    private val norgWebClient: WebClient,
    private val redisService: RedisService,
    private val clientProperties: ClientProperties,
    private val tokendingsService: TokendingsService
) : NorgClient {

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return hentFraCache(enhetsnr) ?: hentFraNorg(enhetsnr)
    }

    private fun hentFraNorg(enhetsnr: String): NavEnhet {
        log.debug("Forsøker å hente NAV-enhet $enhetsnr fra NORG2 (via fss-proxy)")
        val tokenXtoken = runBlocking {
            tokendingsService.exchangeToken(getUserIdFromToken(), getToken(), clientProperties.fssProxyAudience)
        }
        val navEnhet: NavEnhet = norgWebClient.get()
            .uri("/enhet/{enhetsnr}", enhetsnr)
            .accept(MediaType.APPLICATION_JSON)
            .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
            .header(AUTHORIZATION, BEARER + tokenXtoken)
            .retrieve()
            .bodyToMono<NavEnhet>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Noe feilet ved kall mot NORG2 (via fss-proxy) ${e.statusCode}", e)
                NorgException(e.message, e)
            }
            .block()
            ?: throw BadStateException("Ingen feil, men heller ingen NavEnhet")

        log.info("Hentet NAV-enhet $enhetsnr fra NORG2 (via fss-proxy)")

        return navEnhet
            .also { lagreTilCache(enhetsnr, it) }
    }

    private fun hentFraCache(enhetsnr: String): NavEnhet? =
        redisService.get(cacheKey(enhetsnr), NavEnhet::class.java) as NavEnhet?

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
}