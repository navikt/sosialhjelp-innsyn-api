package no.nav.sosialhjelp.innsyn.health.checks

import no.nav.sosialhjelp.innsyn.client.sts.StsClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!local")
@Component
class StsCheck(
    clientProperties: ClientProperties,
    private val stsClient: StsClient
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "STS"
    override val address = clientProperties.stsTokenEndpointUrl
    override val importance = Importance.CRITICAL

    override fun doCheck() {
        stsClient.ping()
    }
}
