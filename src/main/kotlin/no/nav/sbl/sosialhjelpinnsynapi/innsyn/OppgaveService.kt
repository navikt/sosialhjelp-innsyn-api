package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.sosialhjelpinnsynapi.rest.OppgaveFrontend
import org.springframework.stereotype.Component

@Component
class OppgaveService(private val innsynService: InnsynService) {

    fun getOppgaverForSoknad(fiksDigisosId: String): List<OppgaveFrontend> {
        val jsonDigisosSoker = innsynService.hentDigisosSak(fiksDigisosId)
        val oppgaver = mutableListOf<OppgaveFrontend>()
        jsonDigisosSoker.hendelser
                .filter { jsonhendelse -> jsonhendelse.type == JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT }
                .forEach {jsonHendelse -> oppgaver.addAll(getOppgaverFromHendelse(jsonHendelse)) }
        oppgaver.sortBy { it.innsendelsesfrist }
        return oppgaver
    }

    private fun getOppgaverFromHendelse(jsonHendelse: JsonHendelse?) =
            (jsonHendelse as JsonDokumentasjonEtterspurt).dokumenter
                    .mapNotNull { OppgaveFrontend(it.innsendelsesfrist, it.dokumenttype, it.tilleggsinformasjon) }
}