package no.nav.sosialhjelp.innsyn.service.innsyn

import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

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

    private fun isDigisosSakNewerThanMonths(digisosSak: DigisosSak, months: Int): Boolean {
        val testDato = LocalDateTime.now().minusMonths(months.toLong()).toInstant(ZoneOffset.UTC).toEpochMilli()
        return digisosSak.sistEndret >= testDato
    }
}
