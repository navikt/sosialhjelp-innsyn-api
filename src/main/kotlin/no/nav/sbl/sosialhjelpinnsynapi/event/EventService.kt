package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.springframework.stereotype.Component

@Component
class EventService(private val clientProperties: ClientProperties,
                   private val innsynService: InnsynService,
                   private val vedleggService: VedleggService,
                   private val norgClient: NorgClient,
                   private val fiksClient: FiksClient) {

    fun createModel(fiksDigisosId: String, token: String): InternalDigisosSoker {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)

        val jsonDigisosSoker: JsonDigisosSoker? = innsynService.hentJsonDigisosSoker(fiksDigisosId, digisosSak.digisosSoker?.metadata, token)
        val jsonSoknad: JsonSoknad? = innsynService.hentOriginalSoknad(fiksDigisosId, digisosSak.originalSoknadNAV?.metadata, token)
        val timestampSendt = digisosSak.originalSoknadNAV?.timestampSendt

        val model = InternalDigisosSoker()

        if (timestampSendt != null) {
            model.status = SoknadsStatus.SENDT

            if (jsonSoknad != null && jsonSoknad.mottaker != null) {
                model.soknadsmottaker = Soknadsmottaker(jsonSoknad.mottaker.enhetsnummer, jsonSoknad.mottaker.navEnhetsnavn)
                model.historikk.add(Hendelse("Søknaden med vedlegg er sendt til ${jsonSoknad.mottaker.navEnhetsnavn}", unixToLocalDateTime(timestampSendt)))
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
            model.applySoknadKrav(fiksDigisosId, digisosSak.originalSoknadNAV, vedleggService, timestampSendt!!, token)
        }
        model.ettersendtInfoNAV = digisosSak.ettersendtInfoNAV

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
        model.ettersendtInfoNAV = digisosSak.ettersendtInfoNAV
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
            is JsonDokumentasjonkrav -> apply(hendelse)
            is JsonRammevedtak -> apply(hendelse) // Gjør ingenting as of now
            else -> throw RuntimeException("Hendelsetype ${hendelse.type.value()} mangler mapping")
        }
    }
}