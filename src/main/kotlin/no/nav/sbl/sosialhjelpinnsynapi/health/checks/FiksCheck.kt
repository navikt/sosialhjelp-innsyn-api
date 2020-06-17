package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class FiksCheck(
        clientProperties: ClientProperties,
        private val fiksClient: FiksClient
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "Fiks Digisos API"
    override val address = clientProperties.fiksDigisosEndpointUrl
    override val importance = Importance.WARNING

    override fun doCheck() {
        fiksClient.hentKommuneInfoForAlle()
    }

    companion object {
        private val log by logger()
    }
}