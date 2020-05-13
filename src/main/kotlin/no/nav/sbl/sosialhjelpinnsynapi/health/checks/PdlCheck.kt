package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.consumer.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class PdlCheck(
        clientProperties: ClientProperties,
        private val pdlClient: PdlClient
) : AbstractDependencyCheck(
        DependencyType.REST,
        "PDL",
        clientProperties.pdlEndpointUrl,
        Importance.WARNING
) {
    override fun doCheck() {
        pdlClient.ping()
    }
}