package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.springframework.stereotype.Component

@Component
class OppgaveService(private val innsynService: InnsynService) {

    fun getOppgaverForSoknad(fiksDigisosId: String, token: String): List<OppgaveResponse> {
        val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(fiksDigisosId, token) ?: return emptyList()
        return jsonDigisosSoker.hendelser
                .filterIsInstance<JsonDokumentasjonEtterspurt>()
                .flatMap { getOppgaverFromHendelse(it) }
                .sortedBy { it.innsendelsesfrist }
    }

    private fun getOppgaverFromHendelse(hendelse: JsonDokumentasjonEtterspurt) =
            hendelse.dokumenter.mapNotNull { OppgaveResponse(it.innsendelsesfrist, it.dokumenttype, it.tilleggsinformasjon) }
}