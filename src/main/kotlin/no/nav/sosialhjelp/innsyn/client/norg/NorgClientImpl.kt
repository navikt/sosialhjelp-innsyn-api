package no.nav.sosialhjelp.innsyn.client.norg

import no.nav.sosialhjelp.innsyn.common.NorgException
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.NavEnhet
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_NAV_APIKEY
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.forwardHeaders
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate


@Profile("!(mock | local)")
@Component
class NorgClientImpl(
        clientProperties: ClientProperties,
        private val restTemplate: RestTemplate,
        private val redisService: RedisService,
) : NorgClient {

    private val baseUrl = clientProperties.norgEndpointUrl

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        return hentFraCache(enhetsnr) ?: hentFraNorg(enhetsnr)
    }

    private fun hentFraNorg(enhetsnr: String): NavEnhet {
        val headers = headers()
        try {
            log.debug("Forsøker å hente NAV-enhet $enhetsnr fra NORG2")
            val urlTemplate = "$baseUrl/enhet/{enhetsnr}"
            val response = restTemplate.exchange(urlTemplate, HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java, enhetsnr)

            log.info("Hentet NAV-enhet $enhetsnr fra NORG2")
            return objectMapper.readValue(response.body!!, NavEnhet::class.java)
                    .also { lagreTilCache(enhetsnr, it) }

        } catch (e: HttpStatusCodeException) {
            log.warn("Noe feilet ved kall mot NORG2 - ${e.statusCode} ${e.statusText}", e)
            throw NorgException(e.message, e)
        } catch (e: Exception) {
            log.warn("Noe feilet ved kall mot NORG2", e)
            throw NorgException(e.message, e)
        }
    }

    private fun hentFraCache(enhetsnr: String): NavEnhet? =
            redisService.get(cacheKey(enhetsnr), NavEnhet::class.java) as NavEnhet?

    override fun ping() {
        try {
            val headers = headers()
            // samme kall som selftest i soknad-api
            restTemplate.exchange("$baseUrl/kodeverk/EnhetstyperNorg", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        } catch (e: HttpStatusCodeException) {
            log.warn("Selftest - noe feilet ved kall mot NORG2 - ${e.statusCode} ${e.statusText}", e)
            throw NorgException(e.message, e)
        } catch (e: Exception) {
            log.warn("Selftest - noe feilet ved kall mot NORG2", e)
            throw NorgException(e.message, e)
        }
    }

    private fun headers(): HttpHeaders {
        val headers = forwardHeaders()
        headers.set(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
        headers.set(HEADER_NAV_APIKEY, System.getenv(NORG2_APIKEY))
        return headers
    }

    private fun lagreTilCache(enhetsnr: String, navEnhet: NavEnhet) {
        redisService.put(cacheKey(enhetsnr), objectMapper.writeValueAsBytes(navEnhet), NAVENHET_CACHE_TIMETOLIVE_SECONDS)
    }

    private fun cacheKey(enhetsnr: String): String = "NavEnhet_$enhetsnr"

    companion object {
        private val log by logger()

        private const val NORG2_APIKEY = "SOSIALHJELP_INNSYN_API_NORG2_APIKEY_PASSWORD"
        private const val NAVENHET_CACHE_TIMETOLIVE_SECONDS: Long = 60 * 60 // 1 time
    }
}
