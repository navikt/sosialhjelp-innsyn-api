package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.sosialhjelpinnsynapi.rest.OppgaveFrontend
import org.springframework.stereotype.Component

@Component
class OppgaveService(private val innsynService: InnsynService) {

    fun getOppgaverForSoknad(fiksDigisosId: String): List<OppgaveFrontend> {
        val jsonDigisosSoker = innsynService.hentDigisosSak(fiksDigisosId)
        return jsonDigisosSoker.hendelser
                .filterIsInstance<JsonDokumentasjonEtterspurt>()
                .flatMap { getOppgaverFromHendelse(it) }
                .sortedBy { it.innsendelsesfrist }
    }

    private fun getOppgaverFromHendelse(jsonHendelse: JsonHendelse?) =
            (jsonHendelse as JsonDokumentasjonEtterspurt).dokumenter
                    .mapNotNull { OppgaveFrontend(it.innsendelsesfrist, it.dokumenttype, it.tilleggsinformasjon) }
}