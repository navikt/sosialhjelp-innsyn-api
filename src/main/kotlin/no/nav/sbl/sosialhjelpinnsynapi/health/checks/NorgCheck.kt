package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
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
            // potensielt headers etc her
            restTemplate.getForEntity("$address/ping", Any::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke pinge Norg", e)
        }
    }
}