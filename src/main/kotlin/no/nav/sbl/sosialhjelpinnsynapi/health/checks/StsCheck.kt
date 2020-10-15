package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.client.sts.StsClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class StsCheck(
        clientProperties: ClientProperties,
        private val stsClient: StsClient
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "STS"
    override val address = clientProperties.stsTokenEndpointUrl
    override val importance = Importance.WARNING

    override fun doCheck() {
        stsClient.ping()
    }
}