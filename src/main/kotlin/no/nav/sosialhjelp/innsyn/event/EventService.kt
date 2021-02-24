package no.nav.sosialhjelp.innsyn.event

import no.finn.unleash.Unleash
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
import no.nav.sosialhjelp.innsyn.client.norg.NorgClient
import no.nav.sosialhjelp.innsyn.common.VIS_SOKNADEN
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.service.innsyn.InnsynService
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class EventService(
        private val clientProperties: ClientProperties,
        private val innsynService: InnsynService,
        private val vedleggService: VedleggService,
        private val norgClient: NorgClient,
        private val unleashClient: Unleash,
) {

    fun createModel(digisosSak: DigisosSak, token: String): InternalDigisosSoker {
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak.fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
        val jsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(digisosSak.fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)
        val originalSoknadNAV: OriginalSoknadNAV? = digisosSak.originalSoknadNAV
        val dokumentlagerDokumentId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId

        val model = InternalDigisosSoker()

        // Default status == SENDT. Gjelder også for papirsøknader hvor timestampSendt == null
        model.status = SoknadsStatus.SENDT

        if (originalSoknadNAV?.timestampSendt != null) {
            setTidspunktSendtIfNotZero(model, originalSoknadNAV.timestampSendt)
            model.referanse = digisosSak.originalSoknadNAV?.navEksternRefId

            if (jsonSoknad != null && jsonSoknad.mottaker != null) {
                model.soknadsmottaker = Soknadsmottaker(jsonSoknad.mottaker.enhetsnummer, jsonSoknad.mottaker.navEnhetsnavn)
                model.historikk.add(
                        Hendelse(
                                "Søknaden med vedlegg er sendt til ${stripEnhetsnavnForKommune(jsonSoknad.mottaker.navEnhetsnavn)} kommune",
                                unixToLocalDateTime(originalSoknadNAV.timestampSendt),
                                dokumentlagerDokumentId?.let { UrlResponse(VIS_SOKNADEN, hentDokumentlagerUrl(clientProperties, it)) }
                        ))
            }
        }

        applyHendelserOgSoknadKrav(jsonDigisosSoker, model, originalSoknadNAV, digisosSak, token)

        return model
    }

    fun setTidspunktSendtIfNotZero(model: InternalDigisosSoker, timestampSendt: Long) {
        if (timestampSendt == 0L) {
            log.error("Søknadens timestampSendt er 0")
        } else {
            model.tidspunktSendt = unixToLocalDateTime(timestampSendt)
        }
    }

    fun createSaksoversiktModel(digisosSak: DigisosSak, token: String): InternalDigisosSoker {
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak.fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
        val originalSoknadNAV: OriginalSoknadNAV? = digisosSak.originalSoknadNAV

        val model = InternalDigisosSoker()

        if (originalSoknadNAV?.timestampSendt != null) {
            model.status = SoknadsStatus.SENDT
        }

        applyHendelserOgSoknadKrav(jsonDigisosSoker, model, originalSoknadNAV, digisosSak, token)

        return model
    }

    private fun applyHendelserOgSoknadKrav(jsonDigisosSoker: JsonDigisosSoker?, model: InternalDigisosSoker, originalSoknadNAV: OriginalSoknadNAV?, digisosSak: DigisosSak, token: String) {
        jsonDigisosSoker?.hendelser
                ?.sortedWith(hendelseComparator)
                ?.forEach { model.applyHendelse(it) }

        val ingenDokumentasjonskravFraInnsyn = jsonDigisosSoker?.hendelser
                ?.filterIsInstance<JsonDokumentasjonEtterspurt>()
                ?.isEmpty() ?: true

        if (originalSoknadNAV != null && ingenDokumentasjonskravFraInnsyn && soknadSendtForMindreEnn30DagerSiden(originalSoknadNAV.timestampSendt)) {
            model.applySoknadKrav(digisosSak.fiksDigisosId, originalSoknadNAV, vedleggService, originalSoknadNAV.timestampSendt, token)
        }
    }

    fun hentAlleUtbetalinger(token: String, digisosSak: DigisosSak): InternalDigisosSoker {
        val model = InternalDigisosSoker()
        val jsonDigisosSoker: JsonDigisosSoker = innsynService.hentJsonDigisosSoker(digisosSak.fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
                ?: return model
        jsonDigisosSoker.hendelser
                .filterIsInstance<JsonUtbetaling>()
                .sortedBy { it.hendelsestidspunkt }
                .map { model.applyHendelse(it) }
        return model
    }

    private fun InternalDigisosSoker.applyHendelse(hendelse: JsonHendelse) {
        when (hendelse) {
            is JsonSoknadsStatus -> apply(hendelse)
            is JsonTildeltNavKontor -> apply(hendelse, norgClient)
            is JsonSaksStatus -> apply(hendelse)
            is JsonVedtakFattet -> apply(hendelse, clientProperties)
            is JsonDokumentasjonEtterspurt -> apply(hendelse, clientProperties)
            is JsonForelopigSvar -> apply(hendelse, clientProperties)
            is JsonUtbetaling -> apply(hendelse)
            is JsonVilkar -> apply(hendelse)
            is JsonDokumentasjonkrav -> apply(hendelse, unleashClient)
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
        private val hendelseComparator = compareBy<JsonHendelse> { it.hendelsestidspunkt }
                .thenComparator { a, b -> compareHendelseByType(a.type, b.type) }

        private fun compareHendelseByType(a: JsonHendelse.Type, b: JsonHendelse.Type): Int {
            if (a == JsonHendelse.Type.UTBETALING && (b == JsonHendelse.Type.VILKAR || b == JsonHendelse.Type.DOKUMENTASJONKRAV)) {
                return -1
            } else if (b == JsonHendelse.Type.UTBETALING && (a == JsonHendelse.Type.VILKAR || a == JsonHendelse.Type.DOKUMENTASJONKRAV)) {
                return 1
            }
            return 0
        }

        private fun soknadSendtForMindreEnn30DagerSiden(timestampSendt: Long) =
                unixToLocalDateTime(timestampSendt).toLocalDate().isAfter(LocalDate.now().minusDays(30))

        fun stripEnhetsnavnForKommune(navEnhetsnavn: String): String {
            return navEnhetsnavn.replace(" kommune", "")
        }
    }
}