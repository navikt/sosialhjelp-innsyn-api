package no.nav.sbl.sosialhjelpinnsynapi.norg

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(NorgClient::class.java)

@Profile("!mock")
@Component
class NorgClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate = RestTemplate()): NorgClient {

    private val baseUrl = clientProperties.norgEndpointUrl
    private val mapper = ObjectMapper()

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {

        val response = restTemplate.getForEntity("$baseUrl/enhet/$enhetsnr", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet NAV-enhet fra NORG")
            return mapper.readValue(response.body!!, NavEnhet::class.java)
        } else {
            log.warn("Noe feilet ved kall mot NORG")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}
