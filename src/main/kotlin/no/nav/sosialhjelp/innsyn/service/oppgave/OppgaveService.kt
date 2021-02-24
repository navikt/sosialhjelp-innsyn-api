package no.nav.sosialhjelp.innsyn.service.oppgave

import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.OppgaveElement
import no.nav.sosialhjelp.innsyn.domain.OppgaveResponse
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component


@Component
class OppgaveService(
        private val eventService: EventService,
        private val vedleggService: VedleggService,
        private val fiksClient: FiksClient
) {

    fun hentOppgaver(fiksDigisosId: String, token: String): List<OppgaveResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg = vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)

        val oppgaveResponseList = model.oppgaver
                .filter { !erAlleredeLastetOpp(it, ettersendteVedlegg) }
                .groupBy { if (it.innsendelsesfrist == null) null else it.innsendelsesfrist!!.toLocalDate() }
                .map { (key, value) ->
                    OppgaveResponse(
                            innsendelsesfrist = key,
                            oppgaveId = value[0].oppgaveId,  // oppgaveId og innsendelsefrist er alltid 1-1
                            oppgaveElementer = value.map { OppgaveElement(it.tittel, it.tilleggsinfo, it.erFraInnsyn) }
                    )
                }
                .sortedBy { it.innsendelsesfrist }
        log.info("Hentet ${oppgaveResponseList.sumBy { it.oppgaveElementer.size }} oppgaver")
        return oppgaveResponseList
    }

    fun hentOppgaverMedOppgaveId(fiksDigisosId: String, token: String, oppgaveId: String): List<OppgaveResponse> {
        return hentOppgaver(fiksDigisosId, token).filter { it.oppgaveId == oppgaveId }
    }
  
    private fun erAlleredeLastetOpp(oppgave: Oppgave, vedleggListe: List<InternalVedlegg>): Boolean {

        return vedleggListe
                .filter { it.type == oppgave.tittel }
                .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
                .any { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }
    }

    companion object {
        private val log by logger()
    }
}