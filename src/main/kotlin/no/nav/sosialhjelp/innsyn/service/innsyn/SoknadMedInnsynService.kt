package no.nav.sosialhjelp.innsyn.service.innsyn

import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.isNewerThanMonths
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.springframework.stereotype.Component

@Component
class SoknadMedInnsynService(
    private val fiksClient: FiksClient,
    private val kommuneService: KommuneService
) {

    fun harSoknaderMedInnsyn(token: String): Boolean {
        return fiksClient.hentAlleDigisosSaker(token)
            .filter { it.isNewerThanMonths(15) }
            .any { !kommuneService.erInnsynDeaktivertForKommune(it.fiksDigisosId, token) }
    }
}
