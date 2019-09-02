package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.Soknadsmottaker
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import org.springframework.stereotype.Component

@Component
class EventService(private val clientProperties: ClientProperties,
                   private val innsynService: InnsynService,
                   private val norgClient: NorgClient) {

    fun createModel(fiksDigisosId: String, token: String): InternalDigisosSoker {
        val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(fiksDigisosId, token)
        val jsonSoknadsmottaker = innsynService.hentOriginalSoknad(fiksDigisosId).mottaker
        val timestampSendt = innsynService.hentInnsendingstidspunktForOriginalSoknad(fiksDigisosId)

        val internal = InternalDigisosSoker()

        if (jsonSoknadsmottaker != null) {
            internal.soknadsmottaker = Soknadsmottaker(jsonSoknadsmottaker.enhetsnummer, jsonSoknadsmottaker.navEnhetsnavn)
            internal.status = SoknadsStatus.SENDT
            internal.historikk.add(Hendelse("Søknaden med vedlegg er sendt til ${jsonSoknadsmottaker.navEnhetsnavn}", unixToLocalDateTime(timestampSendt)))
        }

        if (jsonDigisosSoker == null) {
            return internal
        }

        jsonDigisosSoker.hendelser
                .sortedBy { it.hendelsestidspunkt }
                .forEach { internal.applyHendelse(it) }

        return internal
    }

    fun InternalDigisosSoker.applyHendelse(hendelse: JsonHendelse) {
        when (hendelse) {
            is JsonSoknadsStatus -> apply(hendelse)
            is JsonTildeltNavKontor -> apply(hendelse, norgClient)
            is JsonSaksStatus -> apply(hendelse)
            is JsonVedtakFattet -> apply(hendelse, clientProperties)
            is JsonDokumentasjonEtterspurt -> apply(hendelse, clientProperties)
            is JsonForelopigSvar -> apply(hendelse, clientProperties)
            is JsonUtbetaling -> apply(hendelse, clientProperties)
            else -> throw RuntimeException("Hendelsetype ${hendelse.type.value()} mangler mapping")
        }
    }
}