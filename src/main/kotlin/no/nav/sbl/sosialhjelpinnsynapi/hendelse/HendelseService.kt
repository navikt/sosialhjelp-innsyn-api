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

    private fun mapToHendelseResponse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker, saker: Map<String, List<JsonSaksStatus>>): HendelseResponse? {
        if (jsonHendelse.type == null) {
            throw RuntimeException("Hendelse mangler type")
        }
        return when (jsonHendelse.type) {
            JsonHendelse.Type.TILDELT_NAV_KONTOR -> tildeltNavKontorHendelse(jsonHendelse, soknadsmottaker)
            JsonHendelse.Type.SOKNADS_STATUS -> soknadsStatusHendelse(jsonHendelse, soknadsmottaker)
            JsonHendelse.Type.VEDTAK_FATTET -> vedtakFattetHendelse(jsonHendelse, saker)
            JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT -> dokumentasjonEtterspurtHendelse(jsonHendelse)
            JsonHendelse.Type.FORELOPIG_SVAR -> forelopigSvarHendelse(jsonHendelse)
            JsonHendelse.Type.SAKS_STATUS -> saksStatusHendelse(jsonHendelse)
            else -> throw RuntimeException("Hendelsestype ${jsonHendelse.type.value()} mangler mapping")
        }
    }

    private fun tildeltNavKontorHendelse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker): HendelseResponse? {
        jsonHendelse as JsonTildeltNavKontor
        if (jsonHendelse.navKontor == soknadsmottaker.enhetsnummer) {
            return null
        }
        val navKontorNavn = norgClient.hentNavEnhet(jsonHendelse.navKontor).navn
        val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt ved $navKontorNavn. Videresendingen vil ikke påvirke saksbehandlingstiden"
        return HendelseResponse(jsonHendelse.hendelsestidspunkt, beskrivelse, null)
    }

    private fun soknadsStatusHendelse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker): HendelseResponse {
        jsonHendelse as JsonSoknadsStatus
        if (jsonHendelse.status == null) {
            throw RuntimeException("JsonSoknadsStatus mangler status")
        }
        val beskrivelse = when (jsonHendelse.status) {
            JsonSoknadsStatus.Status.MOTTATT -> "Søknaden med vedlegg er mottatt hos ${soknadsmottaker.navEnhetsnavn}"
            JsonSoknadsStatus.Status.UNDER_BEHANDLING -> "Søknaden er under behandling"
            JsonSoknadsStatus.Status.FERDIGBEHANDLET -> "Søknaden er ferdig behandlet"
            else -> throw RuntimeException("Statustype ${jsonHendelse.status.value()} mangler mapping")
        }

        return HendelseResponse(jsonHendelse.hendelsestidspunkt, beskrivelse, null)
    }

    private fun vedtakFattetHendelse(jsonHendelse: JsonHendelse, saker: Map<String, List<JsonSaksStatus>>): HendelseResponse {
        jsonHendelse as JsonVedtakFattet
        if (jsonHendelse.utfall == null) {
            return HendelseResponse(jsonHendelse.hendelsestidspunkt, "Vedtak fattet", hentUrlFraFilreferanse(clientProperties, jsonHendelse.vedtaksfil.referanse))
        }

        val utfall = jsonHendelse.utfall.utfall.value().toLowerCase().replace('_', ' ')
        val beskrivelse = if (jsonHendelse.referanse != null && saker.containsKey(jsonHendelse.referanse)) {
            "${saker.getValue(jsonHendelse.referanse)[0].tittel} er $utfall"
        } else {
            log.warn("Tilhørende SaksstatusHendelse manglet eller manglet tittel på saksstatus")
            "En sak har fått utfallet: $utfall"
        }
        return HendelseResponse(jsonHendelse.hendelsestidspunkt, beskrivelse, hentUrlFraFilreferanse(clientProperties, jsonHendelse.vedtaksfil.referanse))
    }

    private fun dokumentasjonEtterspurtHendelse(jsonHendelse: JsonHendelse): HendelseResponse {
        jsonHendelse as JsonDokumentasjonEtterspurt
        val beskrivelse = "Du må laste opp mer dokumentasjon"
        return HendelseResponse(jsonHendelse.hendelsestidspunkt, beskrivelse, hentUrlFraFilreferanse(clientProperties, jsonHendelse.forvaltningsbrev.referanse))
    }

    private fun forelopigSvarHendelse(jsonHendelse: JsonHendelse): HendelseResponse {
        jsonHendelse as JsonForelopigSvar
        val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"
        return HendelseResponse(jsonHendelse.hendelsestidspunkt, beskrivelse, hentUrlFraFilreferanse(clientProperties, jsonHendelse.forvaltningsbrev.referanse))
    }

    private fun saksStatusHendelse(jsonHendelse: JsonHendelse): HendelseResponse? {
        jsonHendelse as JsonSaksStatus
        if (jsonHendelse.status == null) {
            return null
        } else if (jsonHendelse.tittel == null) {
            return HendelseResponse(jsonHendelse.hendelsestidspunkt, "En sak har ikke innsyn", null)
        }

        val status = jsonHendelse.status.value().toLowerCase().replace('_', ' ')
        val beskrivelse = if (jsonHendelse.status == JsonSaksStatus.Status.IKKE_INNSYN) {
            "Saken ${jsonHendelse.tittel} har $status"
        } else {
            "Saken ${jsonHendelse.tittel} er $status"
        }
        return HendelseResponse(jsonHendelse.hendelsestidspunkt, beskrivelse, null)
    }
}