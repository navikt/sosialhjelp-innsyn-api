package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonRammevedtak
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSoknadsStatus
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVilkar
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Fagsystem
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.slf4j.Logger
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class EventService(
    private val clientProperties: ClientProperties,
    private val innsynService: InnsynService,
    private val vedleggService: VedleggService,
    private val norgClient: NorgClient,
) {
    suspend fun createModel(digisosSak: DigisosSak): InternalDigisosSoker {
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak)
        val jsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(digisosSak)

        // Infologging fra kommunesplitting av Ålesund
        if (jsonSoknad != null && jsonSoknad.mottaker?.enhetsnummer == null && jsonSoknad.mottaker?.kommunenummer == "1507") {
            log.info(
                """Enhetsnummer=null: 
                | Hva vet vi:
                | enhetsnummer: null -> mismatch mellom norg og Fiks mellom 8 og 9 den 2. januar 2024
                | kommunenummer satt i søknaden: ${jsonSoknad.mottaker?.kommunenummer}
                | sakstatus: ${
                    jsonDigisosSoker?.hendelser?.filterIsInstance<JsonSoknadsStatus>()?.filter {
                        it.status == JsonSoknadsStatus.Status.MOTTATT
                    }
                }
                | ${
                    jsonDigisosSoker?.hendelser?.filterIsInstance<JsonTildeltNavKontor>()?.map {
                        "Tildelt navkontor: ${it.navKontor} på tidspunkt: ${it.hendelsestidspunkt}"
                    } ?: emptyList()
                }
                """.trimMargin(),
            )
        }
        val originalSoknadNAV: OriginalSoknadNAV? = digisosSak.originalSoknadNAV
        val dokumentlagerDokumentId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId

        val model = InternalDigisosSoker()

        if (jsonDigisosSoker?.avsender != null) {
            model.fagsystem = Fagsystem(jsonDigisosSoker.avsender.systemnavn, jsonDigisosSoker.avsender.systemversjon)
        }

        // Hvis søknad er papirsøknad, vil 'originalSoknad' være null:
        if (originalSoknadNAV?.timestampSendt != null) {
            setTidspunktSendtIfNotZero(model, originalSoknadNAV.timestampSendt)
            model.referanse = digisosSak.originalSoknadNAV?.navEksternRefId
            model.fiksDigisosId = digisosSak.fiksDigisosId

            if (jsonSoknad != null && jsonSoknad.mottaker != null) {
                model.soknadsmottaker = Soknadsmottaker(jsonSoknad.mottaker.enhetsnummer, jsonSoknad.mottaker.navEnhetsnavn)
                model.historikk.add(
                    Hendelse(
                        HendelseTekstType.SOKNAD_SEND_TIL_KONTOR,
                        unixToLocalDateTime(originalSoknadNAV.timestampSendt),
                        dokumentlagerDokumentId?.let {
                            UrlResponse(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR_LENKETEKST, hentDokumentlagerUrl(clientProperties, it))
                        },
                        tekstArgument = stripEnhetsnavnForKommune(jsonSoknad.mottaker.navEnhetsnavn),
                    ),
                )
            }
        }

        applyHendelserOgSoknadKrav(jsonDigisosSoker, model, digisosSak)

        return model
    }

    fun logTekniskSperre(
        jsonDigisosSoker: JsonDigisosSoker?,
        model: InternalDigisosSoker,
        digisosSak: DigisosSak,
        log: Logger,
    ) {
        model.utbetalinger
            .filter { it.forfallsDato?.isBefore(LocalDate.now().minusDays(1)) ?: false }
            .forEach { utbetaling ->
                val sluttdato = utbetaling.utbetalingsDato ?: utbetaling.stoppetDato ?: LocalDate.now()
                val forfallsDato = utbetaling.forfallsDato
                if (forfallsDato != null) {
                    val eventListe = mutableListOf<String>()
                    var opprettelsesdato = LocalDate.now()
                    jsonDigisosSoker
                        ?.hendelser
                        ?.filterIsInstance<JsonUtbetaling>()
                        ?.filter { it.utbetalingsreferanse.equals(utbetaling.referanse) }
                        ?.forEach {
                            eventListe.add("{\"tidspunkt\": \"${it.hendelsestidspunkt}\", \"status\": \"${it.status}\"}")
                            opprettelsesdato = minOf(it.hendelsestidspunkt.toLocalDateTime().toLocalDate(), opprettelsesdato)
                        }
                    val startdato = maxOf(forfallsDato, opprettelsesdato)
                    val overdueDays = ChronoUnit.DAYS.between(startdato, sluttdato)
                    val tilbakevirkende = opprettelsesdato.isAfter(forfallsDato)
                    log.info(
                        "Utbetaling på overtid: {\"referanse\": \"${utbetaling.referanse}\", " +
                            "\"digisosId\": \"${digisosSak.fiksDigisosId}\", " +
                            "\"status\": \"${utbetaling.status.name}\", " +
                            "\"tilbakevirkende\": \"$tilbakevirkende\", \"overdueDays\": \"$overdueDays\", " +
                            "\"utbetalingsDato\": \"${utbetaling.utbetalingsDato}\", \"forfallsdato\": \"${forfallsDato}\", " +
                            "\"kommunenummer\": \"${digisosSak.kommunenummer}\", \"eventer\": $eventListe}",
                    )
                }
            }
    }

    fun setTidspunktSendtIfNotZero(
        model: InternalDigisosSoker,
        timestampSendt: Long,
    ) {
        if (timestampSendt == 0L) {
            log.error("Søknadens timestampSendt er 0")
        } else {
            model.tidspunktSendt = unixToLocalDateTime(timestampSendt)
        }
    }

    suspend fun createSaksoversiktModel(digisosSak: DigisosSak): InternalDigisosSoker {
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak)
        val originalSoknadNAV: OriginalSoknadNAV? = digisosSak.originalSoknadNAV

        val model = InternalDigisosSoker()

        if (originalSoknadNAV?.timestampSendt != null) {
            model.status = SoknadsStatus.SENDT
        }

        applyHendelserOgSoknadKrav(jsonDigisosSoker, model, digisosSak)
        logTekniskSperre(jsonDigisosSoker, model, digisosSak, log)

        return model
    }

    private suspend fun applyHendelserOgSoknadKrav(
        jsonDigisosSoker: JsonDigisosSoker?,
        model: InternalDigisosSoker,
        digisosSak: DigisosSak,
    ) {
        jsonDigisosSoker
            ?.hendelser
            ?.sortedWith(hendelseComparator)
            ?.forEach { model.applyHendelse(it, digisosSak.originalSoknadNAV == null) }

        val ingenDokumentasjonskravFraInnsyn =
            jsonDigisosSoker
                ?.hendelser
                ?.filterIsInstance<JsonDokumentasjonEtterspurt>()
                ?.isEmpty() ?: true

        val originalSoknadNAV = digisosSak.originalSoknadNAV
        if (originalSoknadNAV != null &&
            ingenDokumentasjonskravFraInnsyn &&
            soknadSendtForMindreEnn30DagerSiden(originalSoknadNAV.timestampSendt)
        ) {
            model.applySoknadKrav(digisosSak, vedleggService, originalSoknadNAV.timestampSendt)
        }
    }

    suspend fun hentAlleUtbetalinger(digisosSak: DigisosSak): InternalDigisosSoker {
        val model = InternalDigisosSoker()
        val jsonDigisosSoker: JsonDigisosSoker =
            innsynService.hentJsonDigisosSoker(digisosSak)
                ?: return model
        jsonDigisosSoker.hendelser
            .filterIsInstance<JsonUtbetaling>()
            .sortedBy { it.hendelsestidspunkt }
            .map { model.applyHendelse(it, digisosSak.originalSoknadNAV == null) }
        return model
    }

    private suspend fun InternalDigisosSoker.applyHendelse(
        hendelse: JsonHendelse,
        isPapirSoknad: Boolean,
    ) {
        when (hendelse) {
            is JsonSoknadsStatus -> apply(hendelse)
            is JsonTildeltNavKontor -> apply(hendelse, norgClient, isPapirSoknad)
            is JsonSaksStatus -> apply(hendelse)
            is JsonVedtakFattet -> apply(hendelse, clientProperties)
            is JsonDokumentasjonEtterspurt -> apply(hendelse, clientProperties)
            is JsonForelopigSvar -> apply(hendelse, clientProperties)
            is JsonUtbetaling -> apply(hendelse)
            is JsonVilkar -> apply(hendelse)
            is JsonDokumentasjonkrav -> apply(hendelse)
            is JsonRammevedtak -> apply(hendelse) // Gjør ingenting as of now
            else -> throw RuntimeException("Hendelsetype ${hendelse.type.value()} mangler mapping")
        }
    }

    companion object {
        private val log by logger()

        /**
         * Sorter hendelser på hendelsestidspunkt.
         * Hvis to hendelser har identisk hendelsestidspunkt, og én er Utbetaling og den andre er Vilkår eller Dokumentasjonkrav  -> sorter Utbetaling før Vilkår/Dokumentasjonkrav.
         * Dette gjør at vi kan knytte Vilkår/Dokumentasjonkrav til Utbetalingen.
         */
        private val hendelseComparator =
            compareBy<JsonHendelse> { it.hendelsestidspunkt }
                .thenComparator { a, b -> compareHendelseByType(a.type, b.type) }
                .thenComparator { a, b ->
                    if (a is JsonSoknadsStatus && b is JsonSoknadsStatus) {
                        mottattBeforeUnderBehandling(a, b)
                    } else {
                        0
                    }
                }

        private fun mottattBeforeUnderBehandling(
            a: JsonSoknadsStatus,
            b: JsonSoknadsStatus,
        ): Int {
            if (a.status == JsonSoknadsStatus.Status.MOTTATT && b.status == JsonSoknadsStatus.Status.UNDER_BEHANDLING) {
                return -1
            } else if (b.status == JsonSoknadsStatus.Status.MOTTATT && a.status == JsonSoknadsStatus.Status.UNDER_BEHANDLING) {
                return 1
            }
            return 0
        }

        private fun compareHendelseByType(
            a: JsonHendelse.Type,
            b: JsonHendelse.Type,
        ): Int {
            if (a == JsonHendelse.Type.UTBETALING && (b == JsonHendelse.Type.VILKAR || b == JsonHendelse.Type.DOKUMENTASJONKRAV)) {
                return -1
            } else if (b == JsonHendelse.Type.UTBETALING && (a == JsonHendelse.Type.VILKAR || a == JsonHendelse.Type.DOKUMENTASJONKRAV)) {
                return 1
            }
            return 0
        }

        private fun soknadSendtForMindreEnn30DagerSiden(timestampSendt: Long) =
            unixToLocalDateTime(timestampSendt).toLocalDate().isAfter(LocalDate.now().minusDays(30))

        fun stripEnhetsnavnForKommune(navEnhetsnavn: String?): String? = navEnhetsnavn?.replace(" kommune", "")
    }
}
