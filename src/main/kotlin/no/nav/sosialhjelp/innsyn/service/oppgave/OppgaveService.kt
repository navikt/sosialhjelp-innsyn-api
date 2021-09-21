package no.nav.sosialhjelp.innsyn.service.oppgave

import no.finn.unleash.Unleash
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.client.unleash.DOKUMENTASJONKRAV
import no.nav.sosialhjelp.innsyn.client.unleash.VILKAR
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
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

@Component
class OppgaveService(
    private val eventService: EventService,
    private val vedleggService: VedleggService,
    private val fiksClient: FiksClient,
    private val unleashClient: Unleash,
) {

    fun hentOppgaver(fiksDigisosId: String, token: String): List<OppgaveResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.status == SoknadsStatus.FERDIGBEHANDLET || model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)

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
        val newStatus = model.vilkar
            .filter { it.status == Oppgavestatus.RELEVANT || it.status == Oppgavestatus.ANNULLERT }
            .size
        if (newStatus > 0 ) {
            log.info("Hentet ${newStatus} vilkar med nye statuser (RELEVANT / ANNULERT).")
        }

        val vilkarResponseList = model.vilkar
            .filter {
                !it.isEmpty()
                    .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på vilkår er tomt") }
            }
            .filter { it.status != Oppgavestatus.ANNULLERT }
            .map {
                val (tittel, beskrivelse) = it.getTittelOgBeskrivelse()
                VilkarResponse(
                    it.datoLagtTil.toLocalDate(),
                    it.referanse,
                    tittel,
                    beskrivelse,
                    it.getOppgaveStatus()
                )
            }
            .sortedBy { it.hendelsetidspunkt }

        if (unleashClient.isEnabled(VILKAR, false)) {
            log.info("Hentet ${vilkarResponseList.size} vilkar")
            return vilkarResponseList
        }

        return emptyList()
    }

    fun getDokumentasjonkrav(fiksDigisosId: String, token: String): List<DokumentasjonkravResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)
        if (model.dokumentasjonkrav.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)

        // Logger om fagsystemene har tatt i bruk nye statuser
        val newStatus = model.dokumentasjonkrav
            .filter { it.status == Oppgavestatus.RELEVANT || it.status == Oppgavestatus.ANNULLERT || it.status == Oppgavestatus.LEVERT_TIDLIGERE }
            .size
        if (newStatus > 0 ) {
            log.info("Hentet ${newStatus} dokumentasjonkrav med nye statuser (RELEVANT / ANNULERT / LEVERT_TIDLIGERE).")
        }

        val dokumentasjonkravResponseList = model.dokumentasjonkrav
            .filter {
                !it.isEmpty()
                    .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på dokumentasjonkrav er tomt") }
            }
            .filter { !erAlleredeLastetOpp(it, ettersendteVedlegg) }
            .filter { it.status != Oppgavestatus.ANNULLERT }
            .filter { it.status != Oppgavestatus.LEVERT_TIDLIGERE }
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
                            it.getOppgaveStatus()
                        )
                    }
                )
            }
            .sortedWith(compareBy(nullsLast()) { it.frist })

        if (unleashClient.isEnabled(DOKUMENTASJONKRAV, false)) {
            log.info("Hentet ${dokumentasjonkravResponseList.sumOf { it.dokumentasjonkravElementer.size }} dokumentasjonkrav")
            return dokumentasjonkravResponseList
        }

        return emptyList()
    }

    fun getDokumentasjonkravMedId(
        fiksDigisosId: String,
        dokumentasjonkravId: String,
        token: String
    ): List<DokumentasjonkravResponse> {
        val dokumentasjonkrav = getDokumentasjonkrav(fiksDigisosId, token)

        return dokumentasjonkrav.filter { it.dokumentasjonkravId == dokumentasjonkravId }
    }

    private fun erAlleredeLastetOpp(dokumentasjonkrav: Dokumentasjonkrav, vedleggListe: List<InternalVedlegg>): Boolean {
        return vedleggListe
            .filter { it.type == dokumentasjonkrav.tittel }
            .filter { it.tilleggsinfo == dokumentasjonkrav.beskrivelse }
            .any { dokumentasjonkrav.frist == null || it.tidspunktLastetOpp.isAfter(dokumentasjonkrav.datoLagtTil) }
    }

    companion object {
        private val log by logger()
    }
}
