package no.nav.sbl.sosialhjelpinnsynapi.norg

import no.nav.sbl.sosialhjelpinnsynapi.common.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.generateCallId
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectmapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate


@Profile("!(mock | local)")
@Component
class NorgClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : NorgClient {

    companion object {
        val log by logger()
    }

    private val baseUrl = clientProperties.norgEndpointUrl

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        val norgApiKey = System.getenv("NORG_PASSWORD")
        val headers = HttpHeaders()
        headers.set("Nav-Call-Id", generateCallId())
        headers.set("x-nav-apiKey", norgApiKey)
        try {
            log.info("Forsøker å hente NAV-enhet $enhetsnr fra NORG2")
            val response = restTemplate.exchange("$baseUrl/enhet/$enhetsnr", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)

            log.info("Hentet NAV-enhet $enhetsnr fra NORG2")
            return objectmapper.readValue(response.body!!, NavEnhet::class.java)

        } catch (e: HttpStatusCodeException) {
            log.warn("Noe feilet ved kall mot NORG - ${e.statusCode} ${e.statusText}", e)
            throw NorgException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Noe feilet ved kall mot NORG", e)
            throw NorgException(null, e.message, e)
        }
    }
}
