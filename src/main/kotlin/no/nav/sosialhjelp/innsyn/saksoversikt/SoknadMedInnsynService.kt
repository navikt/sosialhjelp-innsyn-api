package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.isNewerThanMonths
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.springframework.stereotype.Component

@Component
class SoknadMedInnsynService(
    private val fiksService: FiksService,
    private val kommuneService: KommuneService,
) {
    suspend fun harSoknaderMedInnsyn(): Boolean =
        fiksService
            .getAllSoknader()
            .filter { it.isNewerThanMonths(15) }
            .any { !kommuneService.erInnsynDeaktivertForKommune(it.fiksDigisosId) }
}
