package no.nav.sbl.sosialhjelpinnsynapi.norg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.utils.generateCallId
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(NorgClient::class.java)

@Profile("!mock")
@Component
class NorgClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : NorgClient {

    private val baseUrl = clientProperties.norgEndpointUrl
    private val mapper = jacksonObjectMapper()

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        val norgApiKey = System.getProperty("NORG_PASSWORD")
        val headers = HttpHeaders()
        headers.set("Nav-Call-Id", generateCallId())
        headers.set("Nav-Consumer-Id", "srvsoknadsosialhje") // TODO: endre denne når vi har fått generert egen consumer-id for innsyn
        headers.set("x-nav-apiKey", norgApiKey)
        val response = restTemplate.exchange("$baseUrl/enhet/$enhetsnr", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet NAV-enhet fra NORG")
            return mapper.readValue(response.body!!, NavEnhet::class.java)
        } else {
            log.warn("Noe feilet ved kall mot NORG")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}
