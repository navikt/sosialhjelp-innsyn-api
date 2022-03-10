package no.nav.sosialhjelp.innsyn.service.innsyn

import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.dittnav.DittNavOppgaverService.Companion.isDigisosSakNewerThanMonths
import no.nav.sosialhjelp.innsyn.service.kommune.KommuneService
import org.springframework.stereotype.Component

@Component
class SoknadMedInnsynService(
    private val fiksClient: FiksClient,
    private val kommuneService: KommuneService
) {

    fun harSoknaderMedInnsyn(token: String): Boolean {
        val saker = fiksClient.hentAlleDigisosSaker(token)

        return saker
            .filter { isDigisosSakNewerThanMonths(it, 15) }
            .any { !kommuneService.erInnsynDeaktivertForKommune(it.fiksDigisosId, token) }
    }
}
