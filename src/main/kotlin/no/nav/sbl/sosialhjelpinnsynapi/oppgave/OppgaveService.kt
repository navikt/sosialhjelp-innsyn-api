package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveElement
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.springframework.stereotype.Component


@Component
class OppgaveService(private val eventService: EventService,
                     private val vedleggService: VedleggService,
                     private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

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
                            oppgaveElementer = value.map { OppgaveElement(it.tittel, it.tilleggsinfo, it.erFraInnsyn) }
                    )
                }
                .sortedBy { it.innsendelsesfrist }
        log.info("Hentet ${oppgaveResponseList.sumBy { it.oppgaveElementer.size }} oppgaver for digisosId=$fiksDigisosId")
        return oppgaveResponseList
    }

    private fun erAlleredeLastetOpp(oppgave: Oppgave, vedleggListe: List<VedleggService.InternalVedlegg>): Boolean {
        return vedleggListe
                .filter { it.type == oppgave.tittel }
                .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
                .any { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }
    }
}