package no.nav.sbl.sosialhjelpinnsynapi.health.ny

import no.nav.sbl.sosialhjelpinnsynapi.client.sts.StsClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!(mock | local")
@Component
class NyStsCheck(
        clientProperties: ClientProperties,
        private val stsClient: StsClient
) : DependencyCheck(
        DependencyType.REST,
        "STS",
        clientProperties.stsTokenEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        stsClient.ping()
    }
}