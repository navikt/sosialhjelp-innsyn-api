package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType.REST
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance.WARNING
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DokumentlagerCheck(private val restTemplate: RestTemplate,
                         clientProperties: ClientProperties) : AbstractDependencyCheck(
        REST,
        "Dokumentlager",
        clientProperties.fiksDokumentlagerEndpointUrl,
        WARNING
) {

    override fun doCheck() {
        try {
            // potensielt headers etc her
            restTemplate.getForEntity("$address/ping", Any::class.java) // må ha ett reelt endepunkt å kalle
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke pinge Dokumentlager", e)
        }
    }
}