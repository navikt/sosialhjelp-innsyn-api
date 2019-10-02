package no.nav.sbl.sosialhjelpinnsynapi.norg

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.utils.generateCallId
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

private val log = LoggerFactory.getLogger(NorgClient::class.java)

@Profile("!mock & !local")
@Component
class NorgClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : NorgClient {

    private val baseUrl = clientProperties.norgEndpointUrl

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        val norgApiKey = System.getenv("NORG_PASSWORD")
        val headers = HttpHeaders()
        headers.set("Nav-Call-Id", generateCallId())
        headers.set("Nav-Consumer-Id", "srvsosialhjelp-inn")
        headers.set("x-nav-apiKey", norgApiKey)
        val response = restTemplate.exchange("$baseUrl/enhet/$enhetsnr", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet NAV-enhet fra NORG")
            return objectMapper.readValue(response.body!!, NavEnhet::class.java)
        } else {
            log.warn("Noe feilet ved kall mot NORG")
            throw NorgException(response.statusCode, "something went wrong")
        }
    }
}
