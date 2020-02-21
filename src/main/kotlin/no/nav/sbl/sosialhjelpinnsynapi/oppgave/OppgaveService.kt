package no.nav.sbl.sosialhjelpinnsynapi.oppgave

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveElement
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.createHendelseMetric
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.springframework.stereotype.Component


@Component
class OppgaveService(private val eventService: EventService,
                     private val vedleggService: VedleggService,
                     private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

    fun hentOppgaver(digisosId: String, token: String): List<OppgaveResponse> {
        val digisosSak = fiksClient.hentDigisosSak(digisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg = vedleggService.hentEttersendteVedlegg(digisosId, digisosSak.ettersendtInfoNAV, token)

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

        val antallOppgaver = oppgaveResponseList.sumBy { it.oppgaveElementer.size }
        log.info("Hentet $antallOppgaver oppgaver for digisosId=$digisosId")
        createHendelseMetric("sosialhjelp.innsyn.oppgaver", JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT, digisosSak, model)
                .addFieldToReport("antallEtterspurte", model.oppgaver.size.toString())
                .addFieldToReport("antallGjenstaende", antallOppgaver.toString())
                .report()

        return oppgaveResponseList
    }

    private fun erAlleredeLastetOpp(oppgave: Oppgave, vedleggListe: List<VedleggService.InternalVedlegg>): Boolean {
        return vedleggListe
                .filter { it.type == oppgave.tittel }
                .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
                .any { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }
    }
}