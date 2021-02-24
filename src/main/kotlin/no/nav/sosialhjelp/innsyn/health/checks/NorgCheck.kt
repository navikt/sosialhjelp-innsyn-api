package no.nav.sosialhjelp.innsyn.health.checks

import no.nav.sosialhjelp.innsyn.client.norg.NorgClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
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
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "NORG2"
    override val address = clientProperties.norgEndpointUrl
    override val importance = Importance.WARNING

    override fun doCheck() {
        norgClient.ping()
    }

}