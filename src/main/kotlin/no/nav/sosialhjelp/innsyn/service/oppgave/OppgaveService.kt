package no.nav.sosialhjelp.innsyn.service.oppgave

import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.DokumentasjonkravElement
import no.nav.sosialhjelp.innsyn.domain.DokumentasjonkravResponse
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.OppgaveElement
import no.nav.sosialhjelp.innsyn.domain.OppgaveResponse
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.VilkarResponse
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.springframework.stereotype.Component

@Component
class OppgaveService(
    private val eventService: EventService,
    private val vedleggService: VedleggService,
    private val fiksClient: FiksClient,
) {

    fun hentOppgaver(fiksDigisosId: String, token: String): List<OppgaveResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.status == SoknadsStatus.FERDIGBEHANDLET || model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model, token)

        val oppgaveResponseList = model.oppgaver
            .filter { !erAlleredeLastetOpp(it, ettersendteVedlegg) }
            .groupBy { if (it.innsendelsesfrist == null) null else it.innsendelsesfrist!!.toLocalDate() }
            .map { (key, value) ->
                OppgaveResponse(
                    innsendelsesfrist = key,
                    oppgaveId = value[0].oppgaveId, // oppgaveId og innsendelsefrist er alltid 1-1
                    oppgaveElementer = value.map {
                        OppgaveElement(
                            it.tittel,
                            it.tilleggsinfo,
                            it.hendelsetype,
                            it.hendelsereferanse,
                            it.erFraInnsyn
                        )
                    }
                )
            }
            .sortedBy { it.innsendelsesfrist }
        log.info("Hentet ${oppgaveResponseList.sumOf { it.oppgaveElementer.size }} oppgaver")
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

    fun getVilkar(fiksDigisosId: String, token: String): List<VilkarResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.vilkar.isEmpty()) {
            return emptyList()
        }

        // Logger om fagsystemene har tatt i bruk nye statuser
        val antallWithNewStatus = model.vilkar
            .filter { it.status == Oppgavestatus.RELEVANT || it.status == Oppgavestatus.ANNULLERT }
            .size
        if (antallWithNewStatus > 0) {
            log.info("Hentet $antallWithNewStatus vilkar med nye statuser (RELEVANT / ANNULERT).")
        }

        val vilkarResponseList = model.vilkar
            .filter {
                !it.isEmpty()
                    .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på vilkår er tomt") }
            }
            .filter { it.status == Oppgavestatus.RELEVANT }
            .map {
                val (tittel, beskrivelse) = it.getTittelOgBeskrivelse()
                VilkarResponse(
                    it.datoLagtTil.toLocalDate(),
                    it.referanse,
                    tittel,
                    beskrivelse,
                    it.getOppgaveStatus(),
                    it.utbetalingsReferanse
                )
            }
            .sortedBy { it.hendelsetidspunkt }

        log.info("Hentet ${vilkarResponseList.size} vilkar")
        return vilkarResponseList
    }

    fun getDokumentasjonkrav(fiksDigisosId: String, token: String): List<DokumentasjonkravResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.dokumentasjonkrav.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model, token)

        // Logger om fagsystemene har tatt i bruk nye statuser
        val antallWithNewStatus = model.dokumentasjonkrav
            .filter { it.status == Oppgavestatus.RELEVANT || it.status == Oppgavestatus.ANNULLERT || it.status == Oppgavestatus.LEVERT_TIDLIGERE }
            .size
        if (antallWithNewStatus > 0) {
            log.info("Hentet $antallWithNewStatus dokumentasjonkrav med nye statuser (RELEVANT / ANNULERT / LEVERT_TIDLIGERE).")
        }

        val dokumentasjonkravResponseList = model.dokumentasjonkrav
            .asSequence()
            .filter {
                !it.isEmpty()
                    .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på dokumentasjonkrav er tomt") }
            }
            .filter { !erAlleredeLastetOpp(it, ettersendteVedlegg) }
            .filter { it.status == Oppgavestatus.RELEVANT }
            .groupBy { it.frist }
            .map { (key, value) ->
                DokumentasjonkravResponse(
                    dokumentasjonkravId = value[0].dokumentasjonkravId,
                    frist = key,
                    dokumentasjonkravElementer = value.map {
                        val (tittel, beskrivelse) = it.getTittelOgBeskrivelse()
                        DokumentasjonkravElement(
                            it.datoLagtTil.toLocalDate(),
                            it.hendelsetype,
                            it.referanse,
                            tittel,
                            beskrivelse,
                            it.getOppgaveStatus(),
                            utbetalingsReferanse = it.utbetalingsReferanse
                        )
                    }
                )
            }
            .sortedWith(compareBy(nullsLast()) { it.frist })
            .toList()

        log.info("Hentet ${dokumentasjonkravResponseList.sumOf { it.dokumentasjonkravElementer.size }} dokumentasjonkrav")
        return dokumentasjonkravResponseList
    }

    fun getDokumentasjonkravMedId(
        fiksDigisosId: String,
        dokumentasjonkravId: String,
        token: String
    ): List<DokumentasjonkravResponse> {
        val dokumentasjonkrav = getDokumentasjonkrav(fiksDigisosId, token)

        return dokumentasjonkrav.filter { it.dokumentasjonkravId == dokumentasjonkravId }
    }

    private fun erAlleredeLastetOpp(
        dokumentasjonkrav: Dokumentasjonkrav,
        vedleggListe: List<InternalVedlegg>
    ): Boolean {
        return vedleggListe
            .filter { it.type == dokumentasjonkrav.tittel }
            .filter { it.tilleggsinfo == dokumentasjonkrav.beskrivelse }
            .any { dokumentasjonkrav.frist == null || it.tidspunktLastetOpp.isAfter(dokumentasjonkrav.datoLagtTil) }
    }

    fun getHarLevertDokumentasjonkrav(fiksDigisosId: String, token: String): Boolean {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.dokumentasjonkrav.isEmpty()) {
            return false
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model, token)

        return model.dokumentasjonkrav
            .filter {
                !it.isEmpty()
                    .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på dokumentasjonkrav er tomt") }
            }
            .filter { erAlleredeLastetOpp(it, ettersendteVedlegg) }
            .toList().isNotEmpty()
    }

    fun getFagsystemHarVilkarOgDokumentasjonkrav(fiksDigisosId: String, token: String): Boolean {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.dokumentasjonkrav.isEmpty()) {
            return false
        }

        return false
    }

    companion object {
        private val log by logger()
    }
}
