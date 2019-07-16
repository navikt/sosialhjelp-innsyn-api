package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType.REST
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance.WARNING
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DokumentlagerCheck(private val restTemplate: RestTemplate,
                         private val clientProperties: ClientProperties) : AbstractDependencyCheck(
        REST,
        "dokumentlager",
        clientProperties.fiksDokumentlagerEndpointUrl,
        WARNING
) {

    override fun doCheck() {
        val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl
        try {
            // potensielt headers etc her
            restTemplate.getForEntity("$baseUrl/ping", Any::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke pinge dokumentlager", e)
        }
    }
}