package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class PdlCheck(
        clientProperties: ClientProperties,
        private val pdlClient: PdlClient
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "PDL"
    override val address = clientProperties.pdlEndpointUrl
    override val importance = Importance.CRITICAL

    override fun doCheck() {
        pdlClient.ping()
    }
}