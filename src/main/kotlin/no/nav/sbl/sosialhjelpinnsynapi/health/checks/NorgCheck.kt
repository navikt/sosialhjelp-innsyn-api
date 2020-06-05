package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class NorgCheck(
        private val norgClient: NorgClient,
        clientProperties: ClientProperties
) : AbstractDependencyCheck(
        DependencyType.REST,
        "NORG2",
        clientProperties.norgEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        norgClient.ping()
    }

}