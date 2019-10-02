package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import no.nav.sbl.sosialhjelpinnsynapi.utils.generateCallId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Component
class NorgCheck(private val restTemplate: RestTemplate,
                clientProperties: ClientProperties) : AbstractDependencyCheck(
        DependencyType.REST,
        "NORG2",
        clientProperties.norgEndpointUrl,
        Importance.WARNING
) {

    private val log = LoggerFactory.getLogger(NorgCheck::class.java)

    override fun doCheck() {
        try {
            // som i NorgClientImpl
            val norgApiKey = System.getenv("NORG_PASSWORD")
            val headers = HttpHeaders()
            headers.set("Nav-Call-Id", generateCallId())
            headers.set("x-nav-apiKey", norgApiKey)

            // samme kall som selftest i soknad-api utfører
            restTemplate.exchange("$address/kodeverk/EnhetstyperNorg", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        } catch (e: RestClientResponseException) {
            log.error("Kall til Norg feilet", e.responseBodyAsString)
            throw RuntimeException("Selftest-kall mot Norg feilet", e)
        } catch (e: Exception) {
            log.error("Kall til NORG feilet", e)
            throw RuntimeException("Selftest-kall mot Norg feilet", e)
        }
    }
}