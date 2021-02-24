package no.nav.sosialhjelp.innsyn.health.checks

import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.service.idporten.IdPortenService
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!mock")
@Component
class FiksCheck(
    clientProperties: ClientProperties,
    private val kommuneInfoClient: KommuneInfoClient,
    private val idPortenService: IdPortenService
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "Fiks Digisos API"
    override val address = clientProperties.fiksDigisosEndpointUrl
    override val importance = Importance.CRITICAL

    override fun doCheck() {
        kommuneInfoClient.getAll(idPortenService.getToken().token)
    }
}
