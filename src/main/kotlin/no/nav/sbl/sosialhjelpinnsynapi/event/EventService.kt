package no.nav.sbl.sosialhjelpinnsynapi.event

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
import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.common.VIS_SOKNADEN
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.config.FeatureToggles
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.Soknadsmottaker
import no.nav.sbl.sosialhjelpinnsynapi.domain.UrlResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.utils.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.utils.unixToLocalDateTime
import org.springframework.stereotype.Component

@Component
class EventService(
        private val clientProperties: ClientProperties,
        private val innsynService: InnsynService,
        private val vedleggService: VedleggService,
        private val norgClient: NorgClient,
        private val featureToggles: FeatureToggles
) {

    fun createModel(digisosSak: DigisosSak, token: String): InternalDigisosSoker {
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak.fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
        val jsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(digisosSak.fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)
        val timestampSendt = digisosSak.originalSoknadNAV?.timestampSendt
        val dokumentlagerDokumentId: String? = digisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId

        val model = InternalDigisosSoker()

        if (timestampSendt != null) {
            model.status = SoknadsStatus.SENDT

            if (jsonSoknad != null && jsonSoknad.mottaker != null) {
                model.soknadsmottaker = Soknadsmottaker(jsonSoknad.mottaker.enhetsnummer, jsonSoknad.mottaker.navEnhetsnavn)
                model.historikk.add(
                        Hendelse(
                                "Søknaden med vedlegg er sendt til ${jsonSoknad.mottaker.navEnhetsnavn}",
                                unixToLocalDateTime(timestampSendt),
                                dokumentlagerDokumentId?.let { UrlResponse(VIS_SOKNADEN, hentDokumentlagerUrl(clientProperties, it)) }
                        ))
            }
        }

        var ingenDokumentasjonskravFraInnsyn = true
        if (jsonDigisosSoker != null) {
            jsonDigisosSoker.hendelser
                    .sortedBy { it.hendelsestidspunkt }
                    .forEach { model.applyHendelse(it) }

            ingenDokumentasjonskravFraInnsyn = jsonDigisosSoker.hendelser.filterIsInstance<JsonDokumentasjonEtterspurt>().isEmpty()
        }

        if (digisosSak.originalSoknadNAV != null && ingenDokumentasjonskravFraInnsyn) {
            model.applySoknadKrav(digisosSak.fiksDigisosId, digisosSak.originalSoknadNAV, vedleggService, timestampSendt!!, token)
        }

        return model
    }

    fun createSaksoversiktModel(token: String, digisosSak: DigisosSak): InternalDigisosSoker {
        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(digisosSak.fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
        val timestampSendt = digisosSak.originalSoknadNAV?.timestampSendt

        val model = InternalDigisosSoker()
        if (timestampSendt != null) {
            model.status = SoknadsStatus.SENDT
        }
        if (jsonDigisosSoker == null) {
            return model
        }
        jsonDigisosSoker.hendelser
                .sortedBy { it.hendelsestidspunkt }
                .forEach { model.applyHendelse(it) }

        return model
    }

    fun hentAlleUtbetalinger(token: String, digisosSak: DigisosSak): InternalDigisosSoker {
        val model = InternalDigisosSoker()
        val jsonDigisosSoker: JsonDigisosSoker = innsynService.hentJsonDigisosSoker(digisosSak.fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
                ?: return model
        jsonDigisosSoker.hendelser
                .sortedBy { it.hendelsestidspunkt }
                .filter { it is JsonUtbetaling }
                .map { model.applyHendelse(it) }
        return model
    }

    fun InternalDigisosSoker.applyHendelse(hendelse: JsonHendelse) {
        when (hendelse) {
            is JsonSoknadsStatus -> apply(hendelse)
            is JsonTildeltNavKontor -> apply(hendelse, norgClient)
            is JsonSaksStatus -> apply(hendelse)
            is JsonVedtakFattet -> apply(hendelse, clientProperties)
            is JsonDokumentasjonEtterspurt -> apply(hendelse, clientProperties)
            is JsonForelopigSvar -> apply(hendelse, clientProperties)
            is JsonUtbetaling -> apply(hendelse)
            is JsonVilkar -> apply(hendelse)
            is JsonDokumentasjonkrav -> apply(hendelse, featureToggles)
            is JsonRammevedtak -> apply(hendelse) // Gjør ingenting as of now
            else -> throw RuntimeException("Hendelsetype ${hendelse.type.value()} mangler mapping")
        }
    }
}