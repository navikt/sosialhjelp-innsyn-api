package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class NorgCheck(
        private val norgClient: NorgClient,
        clientProperties: ClientProperties
) : DependencyCheck(
        type = DependencyType.REST,
        name = "NORG2",
        address = clientProperties.norgEndpointUrl,
        importance = Importance.WARNING
) {

    override fun doCheck() {
        norgClient.ping()
    }

}