package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import com.fasterxml.jackson.core.util.VersionUtil
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.Fagsystem
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.springframework.stereotype.Component
import kotlin.collections.sortedWith
import kotlin.comparisons.nullsLast

@Component
class OppgaveService(
    private val eventService: EventService,
    private val vedleggService: VedleggService,
    private val fiksService: FiksService,
    private val clientProperties: ClientProperties,
    private val meterRegistry: MeterRegistry,
) {
    private val oppgaveTeller = Counter.builder("oppgave_teller")

    suspend fun hentOppgaverBeta(fiksDigisosId: String): List<OppgaveResponseBeta> {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.status == SoknadsStatus.FERDIGBEHANDLET || model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model)

        val oppgaveResponseList =
            model.oppgaver
                .map { oppgave ->
                    val alleredeLastetOpp = finnAlleredeLastetOpp(oppgave, ettersendteVedlegg)
                    OppgaveResponseBeta(
                        innsendelsesfrist = oppgave.innsendelsesfrist?.toLocalDate(),
                        oppgaveId = oppgave.oppgaveId,
                        dokumenttype = oppgave.tittel,
                        tilleggsinformasjon = oppgave.tilleggsinfo,
                        hendelsetype = oppgave.hendelsetype,
                        hendelsereferanse = oppgave.hendelsereferanse,
                        erFraInnsyn = oppgave.erFraInnsyn,
                        erLastetOpp = alleredeLastetOpp.isNotEmpty(),
                        opplastetDato = alleredeLastetOpp.firstOrNull()?.tidspunktLastetOpp,
                    )
                }.sortedWith(compareBy<OppgaveResponseBeta> { it.erLastetOpp }.thenBy { it.innsendelsesfrist })
        log.info("Hentet ${oppgaveResponseList.size} oppgaver")
        oppgaveTeller.tag("fiksDigisosId", fiksDigisosId).register(meterRegistry).increment(oppgaveResponseList.size.toDouble())
        return oppgaveResponseList
    }

    @Deprecated("Gammel funksjon", replaceWith = ReplaceWith("hentOppgaverBeta(fiksDigisosId)"))
    suspend fun hentOppgaver(fiksDigisosId: String): List<OppgaveResponse> {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.status == SoknadsStatus.FERDIGBEHANDLET || model.oppgaver.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model)

        val oppgaveResponseList =
            model.oppgaver
                .filter { oppgave ->
                    finnAlleredeLastetOpp(
                        oppgave,
                        ettersendteVedlegg,
                    ).isEmpty()
                }.groupBy { it.innsendelsesfrist?.toLocalDate() }
                .map { (key, value) ->
                    OppgaveResponse(
                        innsendelsesfrist = key,
                        // oppgaveId og innsendelsefrist er alltid 1-1
                        oppgaveId = value[0].oppgaveId,
                        oppgaveElementer =
                            value.map {
                                OppgaveElement(
                                    it.tittel,
                                    it.tilleggsinfo,
                                    it.hendelsetype,
                                    it.hendelsereferanse,
                                    it.erFraInnsyn,
                                )
                            },
                    )
                }.sortedBy { it.innsendelsesfrist }
        log.info("Hentet ${oppgaveResponseList.sumOf { it.oppgaveElementer.size }} oppgaver")
        oppgaveTeller.tag("fiksDigisosId", fiksDigisosId).register(meterRegistry).increment(oppgaveResponseList.size.toDouble())
        return oppgaveResponseList
    }

    suspend fun hentOppgaverMedOppgaveId(
        fiksDigisosId: String,
        oppgaveId: String,
    ): List<OppgaveResponse> = hentOppgaver(fiksDigisosId).filter { it.oppgaveId == oppgaveId }

    private fun finnAlleredeLastetOpp(
        oppgave: Oppgave,
        vedleggListe: List<InternalVedlegg>,
    ): List<InternalVedlegg> =
        vedleggListe
            .filter { it.type == oppgave.tittel }
            .filter { it.tilleggsinfo == oppgave.tilleggsinfo }
            .filter { it.tidspunktLastetOpp.isAfter(oppgave.tidspunktForKrav) }

    suspend fun getVilkar(fiksDigisosId: String): List<VilkarResponse> {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.vilkar.isEmpty()) {
            return emptyList()
        }

        // Logger om fagsystemene har tatt i bruk nye statuser
        val antallWithNewStatus =
            model.vilkar
                .filter { it.status == Oppgavestatus.RELEVANT || it.status == Oppgavestatus.ANNULLERT }
                .size
        if (antallWithNewStatus > 0) {
            log.info("Hentet $antallWithNewStatus vilkar med nye statuser (RELEVANT / ANNULERT).")
        }

        val vilkarResponseList =
            model.vilkar
                .filter {
                    !it
                        .isEmpty()
                        .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på vilkår er tomt") }
                }.filter { it.status == Oppgavestatus.RELEVANT }
                .map {
                    val (tittel, beskrivelse) = it.getTittelOgBeskrivelse()
                    VilkarResponse(
                        it.datoLagtTil.toLocalDate(),
                        it.referanse,
                        tittel,
                        beskrivelse,
                        it.getOppgaveStatus(),
                        it.utbetalingsReferanse,
                        it.saksReferanse,
                    )
                }.sortedBy { it.hendelsetidspunkt }

        log.info("Hentet ${vilkarResponseList.size} vilkar")
        return vilkarResponseList
    }

    suspend fun getDokumentasjonkravBeta(fiksDigisosId: String): List<DokumentasjonkravDto> {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.dokumentasjonkrav.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model)

        // Logger om fagsystemene har tatt i bruk nye statuser
        val antallWithNewStatus =
            model.dokumentasjonkrav
                .count {
                    it.status in listOf(Oppgavestatus.RELEVANT, Oppgavestatus.ANNULLERT, Oppgavestatus.LEVERT_TIDLIGERE)
                }
        if (antallWithNewStatus > 0) {
            log.info("Hentet $antallWithNewStatus dokumentasjonkrav med nye statuser (RELEVANT / ANNULERT / LEVERT_TIDLIGERE).")
        }

        val dokumentasjonkravResponseList =
            model.dokumentasjonkrav
                .asSequence()
                .filter {
                    !it
                        .isEmpty()
                        .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på dokumentasjonkrav er tomt") }
                }.filter { it.status == Oppgavestatus.RELEVANT }
                .map {
                    val (tittel, beskrivelse) = it.getTittelOgBeskrivelse()
                    val alleredeLastetOpp = finnAlleredeLastetOpp(it, ettersendteVedlegg)
                    DokumentasjonkravDto(
                        dokumentasjonkravId = it.dokumentasjonkravId,
                        frist = it.frist,
                        hendelsetidspunkt = it.datoLagtTil.toLocalDate(),
                        hendelsetype = it.hendelsetype,
                        dokumentasjonkravReferanse = it.referanse,
                        tittel = tittel,
                        beskrivelse = beskrivelse,
                        status = it.getOppgaveStatus(),
                        utbetalingsReferanse = it.utbetalingsReferanse ?: emptyList(),
                        saksreferanse = it.saksreferanse,
                        erLastetOpp = alleredeLastetOpp.isNotEmpty(),
                        opplastetDato = alleredeLastetOpp.firstOrNull()?.tidspunktLastetOpp,
                    )
                }.sortedWith(
                    compareBy<DokumentasjonkravDto> {
                        it.erLastetOpp
                    }.thenBy(nullsLast()) { it.frist },
                ).toList()

        log.info("Hentet ${dokumentasjonkravResponseList.size} dokumentasjonkrav")
        return dokumentasjonkravResponseList
    }

    @Deprecated("Gammel funksjon", replaceWith = ReplaceWith("getDokumentasjonkravBeta(fiksDigisosId)"))
    suspend fun getDokumentasjonkrav(fiksDigisosId: String): List<DokumentasjonkravResponse> {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.dokumentasjonkrav.isEmpty()) {
            return emptyList()
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model)

        // Logger om fagsystemene har tatt i bruk nye statuser
        val antallWithNewStatus =
            model.dokumentasjonkrav
                .count {
                    it.status in listOf(Oppgavestatus.RELEVANT, Oppgavestatus.ANNULLERT, Oppgavestatus.LEVERT_TIDLIGERE)
                }
        if (antallWithNewStatus > 0) {
            log.info("Hentet $antallWithNewStatus dokumentasjonkrav med nye statuser (RELEVANT / ANNULERT / LEVERT_TIDLIGERE).")
        }

        val dokumentasjonkravResponseList =
            model.dokumentasjonkrav
                .asSequence()
                .filter {
                    !it
                        .isEmpty()
                        .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på dokumentasjonkrav er tomt") }
                }.filter { finnAlleredeLastetOpp(it, ettersendteVedlegg).isEmpty() }
                .filter { it.status == Oppgavestatus.RELEVANT }
                .groupBy { it.frist }
                .map { (key, value) ->
                    DokumentasjonkravResponse(
                        dokumentasjonkravId = value[0].dokumentasjonkravId,
                        frist = key,
                        dokumentasjonkravElementer =
                            value.map {
                                val (tittel, beskrivelse) = it.getTittelOgBeskrivelse()
                                DokumentasjonkravElement(
                                    it.datoLagtTil.toLocalDate(),
                                    it.hendelsetype,
                                    it.referanse,
                                    tittel,
                                    beskrivelse,
                                    it.getOppgaveStatus(),
                                    utbetalingsReferanse = it.utbetalingsReferanse,
                                )
                            },
                    )
                }.sortedWith(compareBy(nullsLast()) { it.frist })
                .toList()

        log.info("Hentet ${dokumentasjonkravResponseList.sumOf { it.dokumentasjonkravElementer.size }} dokumentasjonkrav")
        return dokumentasjonkravResponseList
    }

    suspend fun getDokumentasjonkravMedId(
        fiksDigisosId: String,
        dokumentasjonkravId: String,
    ): List<DokumentasjonkravResponse> {
        val dokumentasjonkrav = getDokumentasjonkrav(fiksDigisosId)

        return dokumentasjonkrav.filter { it.dokumentasjonkravId == dokumentasjonkravId }
    }

    private fun finnAlleredeLastetOpp(
        dokumentasjonkrav: Dokumentasjonkrav,
        vedleggListe: List<InternalVedlegg>,
    ): List<InternalVedlegg> =
        vedleggListe
            .filter { it.type == dokumentasjonkrav.tittel }
            .filter { it.tilleggsinfo == dokumentasjonkrav.beskrivelse }
            .filter { it.tidspunktLastetOpp.isAfter(dokumentasjonkrav.datoLagtTil) || dokumentasjonkrav.frist == null }

    suspend fun getHarLevertDokumentasjonkrav(fiksDigisosId: String): Boolean {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.dokumentasjonkrav.isEmpty()) {
            return false
        }

        val ettersendteVedlegg =
            vedleggService.hentEttersendteVedlegg(digisosSak, model)

        return model.dokumentasjonkrav
            .filter {
                !it
                    .isEmpty()
                    .also { isEmpty -> if (isEmpty) log.error("Tittel og beskrivelse på dokumentasjonkrav er tomt") }
            }.filter { finnAlleredeLastetOpp(it, ettersendteVedlegg).isNotEmpty() }
            .toList()
            .isNotEmpty()
    }

    suspend fun getFagsystemHarVilkarOgDokumentasjonkrav(fiksDigisosId: String): Boolean {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.fagsystem == null || model.fagsystem!!.systemversjon == null || model.fagsystem!!.systemnavn == null) {
            return false
        }

        val fagsystemer =
            clientProperties.vilkarDokkravFagsystemVersjoner.mapNotNull {
                try {
                    val split = it.split(";")
                    Fagsystem(split[0], split[1])
                } catch (e: IndexOutOfBoundsException) {
                    log.error("Kan ikke splitte fagsystem-versjon i app config $it")
                    null
                }
            }

        return fagsystemer
            .filter { model.fagsystem!!.systemnavn.equals(it.systemnavn) }
            .any { versionEqualsOrIsNewer(model.fagsystem!!.systemversjon!!, it.systemversjon!!) }
    }

    private fun versionEqualsOrIsNewer(
        avsender: String,
        godkjent: String,
    ): Boolean {
        val avsenderVersion = VersionUtil.parseVersion(avsender, null, null)
        val godkjentVersion = VersionUtil.parseVersion(godkjent, null, null)

        if (avsenderVersion.isUnknownVersion || godkjentVersion.isUnknownVersion) {
            return false
        }

        return avsenderVersion >= godkjentVersion
    }

    suspend fun sakHarStatusMottattOgIkkeHattSendt(fiksDigisosId: String): Boolean {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)
        if (model.status != SoknadsStatus.MOTTATT) {
            return false
        }

        return model.historikk.none { hendelse -> hendelse.hendelseType == HendelseTekstType.SOKNAD_SEND_TIL_KONTOR }
    }

    companion object {
        private val log by logger()
    }
}
