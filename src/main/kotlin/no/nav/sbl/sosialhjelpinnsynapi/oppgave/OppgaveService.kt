package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveElement
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.springframework.stereotype.Component


@Component
class OppgaveService(private val vedleggService: VedleggService) {

    companion object {
        val log by logger()
    }

    fun hentOppgaver(fiksDigisosId: String, model: InternalDigisosSoker, token: String): List<OppgaveResponse> {
        if (model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg = vedleggService.hentEttersendteVedlegg(fiksDigisosId, model.ettersendtInfoNAV, token)

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
        log.info("Hentet ${oppgaveResponseList.sumBy { it.oppgaveElementer.size }} oppgaver for fiksDigisosId=$fiksDigisosId")
        return oppgaveResponseList
    }

    private fun erAlleredeLastetOpp(oppgave: Oppgave, vedleggListe: List<VedleggService.InternalVedlegg>): Boolean {
        return vedleggListe
                .filter { it.type == oppgave.tittel }
                .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
                .any { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }
    }
}