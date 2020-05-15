package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.client.sts.StsClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class StsCheck(
        clientProperties: ClientProperties,
        private val stsClient: StsClient
) : AbstractDependencyCheck(
        DependencyType.REST,
        "STS",
        clientProperties.stsTokenEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        stsClient.ping()
    }
}