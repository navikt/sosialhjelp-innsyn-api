package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class HendelseService(private val clientProperties: ClientProperties,
                      private val innsynService: InnsynService,
                      private val norgClient: NorgClient) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    fun getHendelserForSoknad(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(fiksDigisosId, token)
        val jsonSoknad = innsynService.hentOriginalSoknad(fiksDigisosId)
        val timestampSendtUnix = innsynService.hentInnsendingstidspunktForOriginalSoknad(fiksDigisosId)
        val timestampSendt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestampSendtUnix), ZoneOffset.UTC).toString()
        return createHendelserList(jsonDigisosSoker, jsonSoknad, timestampSendt)
    }

    private fun createHendelserList(jsonDigisosSoker: JsonDigisosSoker?, jsonSoknad: JsonSoknad, timestampSendt: String): List<HendelseResponse> {
        val soknadsmottaker = jsonSoknad.mottaker
        val hendelser = mutableListOf(HendelseResponse(timestampSendt, "Søknaden med vedlegg er sendt til ${soknadsmottaker.navEnhetsnavn}", null))
        if (jsonDigisosSoker == null) {
            return hendelser
        }

        val saker: Map<String, List<JsonSaksStatus>> = mapSaksStatusHendelserToMapOfReferanser(jsonDigisosSoker)
        hendelser.addAll(jsonDigisosSoker.hendelser
                .filterNot { it.type == JsonHendelse.Type.UTBETALING || it.type == JsonHendelse.Type.VILKAR }
                .mapNotNull { mapToHendelseResponse(it, soknadsmottaker, saker) })
        hendelser.sortByDescending { it.tidspunkt }
        return hendelser
    }

    private fun mapSaksStatusHendelserToMapOfReferanser(jsonDigisosSoker: JsonDigisosSoker): Map<String, List<JsonSaksStatus>> {
        return jsonDigisosSoker.hendelser.asSequence().filterIsInstance<JsonSaksStatus>()
                .filterNot { it.tittel.isNullOrBlank() }
                .sortedByDescending { it.hendelsestidspunkt }
                .distinctBy { it.referanse }
                .groupBy { it.referanse }
    }

    private fun mapToHendelseResponse(hendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker, saker: Map<String, List<JsonSaksStatus>>): HendelseResponse? {
        if (hendelse.type == null) {
            throw RuntimeException("Hendelse mangler type")
        }
        return when (hendelse.type) {
            JsonHendelse.Type.TILDELT_NAV_KONTOR -> tildeltNavKontorHendelse(hendelse as JsonTildeltNavKontor, soknadsmottaker)
            JsonHendelse.Type.SOKNADS_STATUS -> soknadsStatusHendelse(hendelse as JsonSoknadsStatus, soknadsmottaker)
            JsonHendelse.Type.VEDTAK_FATTET -> vedtakFattetHendelse(hendelse as JsonVedtakFattet, saker)
            JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT -> dokumentasjonEtterspurtHendelse(hendelse as JsonDokumentasjonEtterspurt)
            JsonHendelse.Type.FORELOPIG_SVAR -> forelopigSvarHendelse(hendelse as JsonForelopigSvar)
            JsonHendelse.Type.SAKS_STATUS -> saksStatusHendelse(hendelse as JsonSaksStatus)
            else -> throw RuntimeException("Hendelsestype ${hendelse.type.value()} mangler mapping")
        }
    }

    private fun tildeltNavKontorHendelse(hendelse: JsonTildeltNavKontor, soknadsmottaker: JsonSoknadsmottaker): HendelseResponse? {
        if (hendelse.navKontor == soknadsmottaker.enhetsnummer) {
            return null
        }
        val navKontorNavn = norgClient.hentNavEnhet(hendelse.navKontor).navn
        val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt ved $navKontorNavn. Videresendingen vil ikke påvirke saksbehandlingstiden"
        return HendelseResponse(hendelse.hendelsestidspunkt, beskrivelse, null)
    }

    private fun soknadsStatusHendelse(hendelse: JsonSoknadsStatus, soknadsmottaker: JsonSoknadsmottaker): HendelseResponse {
        if (hendelse.status == null) {
            throw RuntimeException("JsonSoknadsStatus mangler status")
        }
        val beskrivelse = when (hendelse.status) {
            JsonSoknadsStatus.Status.MOTTATT -> "Søknaden med vedlegg er mottatt hos ${soknadsmottaker.navEnhetsnavn}"
            JsonSoknadsStatus.Status.UNDER_BEHANDLING -> "Søknaden er under behandling"
            JsonSoknadsStatus.Status.FERDIGBEHANDLET -> "Søknaden er ferdig behandlet"
            else -> throw RuntimeException("Statustype ${hendelse.status.value()} mangler mapping")
        }

        return HendelseResponse(hendelse.hendelsestidspunkt, beskrivelse, null)
    }

    private fun vedtakFattetHendelse(hendelse: JsonVedtakFattet, saker: Map<String, List<JsonSaksStatus>>): HendelseResponse {
        if (hendelse.utfall == null) {
            return HendelseResponse(hendelse.hendelsestidspunkt, "Vedtak fattet", hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse))
        }

        val utfall = hendelse.utfall.utfall.value().toLowerCase().replace('_', ' ')
        val beskrivelse = if (hendelse.referanse != null && saker.containsKey(hendelse.referanse)) {
            "${saker.getValue(hendelse.referanse)[0].tittel} er $utfall"
        } else {
            log.warn("Tilhørende SaksstatusHendelse manglet eller manglet tittel på saksstatus")
            "En sak har fått utfallet: $utfall"
        }
        return HendelseResponse(hendelse.hendelsestidspunkt, beskrivelse, hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse))
    }

    private fun dokumentasjonEtterspurtHendelse(hendelse: JsonDokumentasjonEtterspurt): HendelseResponse {
        val beskrivelse = "Du må laste opp mer dokumentasjon"
        return HendelseResponse(hendelse.hendelsestidspunkt, beskrivelse, hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse))
    }

    private fun forelopigSvarHendelse(hendelse: JsonForelopigSvar): HendelseResponse {
        val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"
        return HendelseResponse(hendelse.hendelsestidspunkt, beskrivelse, hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse))
    }

    private fun saksStatusHendelse(hendelse: JsonSaksStatus): HendelseResponse? {
        if (hendelse.status == null) {
            return null
        } else if (hendelse.tittel == null) {
            return HendelseResponse(hendelse.hendelsestidspunkt, "En sak har ikke innsyn", null)
        }

        val status = hendelse.status.value().toLowerCase().replace('_', ' ')
        val beskrivelse = if (hendelse.status == JsonSaksStatus.Status.IKKE_INNSYN) {
            "Saken ${hendelse.tittel} har $status"
        } else {
            "Saken ${hendelse.tittel} er $status"
        }
        return HendelseResponse(hendelse.hendelsestidspunkt, beskrivelse, null)
    }
}