package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import no.nav.sbl.sosialhjelpinnsynapi.utils.generateCallId
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class NorgCheck(private val restTemplate: RestTemplate,
                clientProperties: ClientProperties) : AbstractDependencyCheck(
        DependencyType.REST,
        "Norg",
        clientProperties.norgEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        try {
            // som i NorgClientImpl
            val norgApiKey = System.getProperty("NORG_PASSWORD")
            val headers = HttpHeaders()
            headers.set("Nav-Call-Id", generateCallId())
            headers.set("Nav-Consumer-Id", "srvsosialhjelp-inn")
            headers.set("x-nav-apiKey", norgApiKey)

            restTemplate.exchange("$address/ping", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java) // må ha ett reelt endepunkt å kalle
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke pinge Norg", e)
        }
    }
}