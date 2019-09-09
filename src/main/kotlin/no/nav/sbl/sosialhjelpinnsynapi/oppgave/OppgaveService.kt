package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(OppgaveService::class.java)

@Component
class OppgaveService(private val eventService: EventService,
                     private val vedleggService: VedleggService,
                     private val fiksClient: FiksClient) {

    fun hentOppgaver(fiksDigisosId: String, token: String): List<OppgaveResponse> {
        val model = eventService.createModel(fiksDigisosId, token)

        if (model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val ettersendteVedlegg = vedleggService.hentEttersendteVedlegg(digisosSak.ettersendtInfoNAV)

        val oppgaveResponseList = model.oppgaver.sortedBy { it.innsendelsesfrist }
                .filter { !erAlleredeLastetOpp(it, ettersendteVedlegg) }
                .map { OppgaveResponse(it.innsendelsesfrist.toString(), it.tittel, it.tilleggsinfo) }
        log.info("Hentet ${oppgaveResponseList.size} oppgaver for fiksDigisosId=$fiksDigisosId")
        return oppgaveResponseList
    }

    private fun erAlleredeLastetOpp(oppgave: Oppgave, vedleggListe: List<VedleggService.InternalVedlegg>): Boolean {
        return vedleggListe
                .filter { it.type == oppgave.tittel }
                .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
                .any { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }
    }
}