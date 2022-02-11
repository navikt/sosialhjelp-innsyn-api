package no.nav.sosialhjelp.innsyn.health.checks

import no.nav.sosialhjelp.innsyn.client.fssproxy.FssProxyClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.stereotype.Component

@Component
class FssProxyCheck(
    private val fssProxyClient: FssProxyClient,
    clientProperties: ClientProperties
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "FssProxy"
    override val address = clientProperties.fssProxyPingUrl
    override val importance = Importance.WARNING

    override fun doCheck() {
        fssProxyClient.ping()
    }
}
