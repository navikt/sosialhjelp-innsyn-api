package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.rest.HendelseFrontend
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class HendelseService(private val innsynService: InnsynService,
                      private val norgClient: NorgClient) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    fun getHendelserForSoknad(fiksDigisosId: String, token: String): List<HendelseFrontend> {
        val jsonDigisosSoker = innsynService.hentJsonDigisosSoker(fiksDigisosId, token)
        val jsonSoknad = innsynService.hentOriginalSoknad(fiksDigisosId)
        val timestampSendtUnix = innsynService.hentInnsendingstidspunktForOriginalSoknad(fiksDigisosId)
        val timestampSendt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestampSendtUnix), ZoneOffset.UTC).toString()
        return createHendelserList(jsonDigisosSoker, jsonSoknad, timestampSendt)
    }

    private fun createHendelserList(jsonDigisosSoker: JsonDigisosSoker?, jsonSoknad: JsonSoknad, timestampSendt: String): List<HendelseFrontend> {
        val hendelser = mutableListOf<HendelseFrontend>()
        val soknadsmottaker = jsonSoknad.mottaker
        hendelser.add(HendelseFrontend(timestampSendt, "Søknaden med vedlegg er sendt til ${soknadsmottaker.navEnhetsnavn}", null, null, null))
        if (jsonDigisosSoker == null) {
            return hendelser
        }

        val saker: MutableMap<String, String> = mapSaksStatusHendelserToMapOfReferanser(jsonDigisosSoker)
        hendelser.addAll(jsonDigisosSoker.hendelser
                .filterNot { it.type == JsonHendelse.Type.UTBETALING || it.type == JsonHendelse.Type.VILKAR }
                .mapNotNull { mapToHendelseFrontend(it, soknadsmottaker, saker) })
        hendelser.sortBy { it.timestamp }
        return hendelser
    }

    private fun mapSaksStatusHendelserToMapOfReferanser(jsonDigisosSoker: JsonDigisosSoker): MutableMap<String, String> {
        val saker: MutableMap<String, String> = mutableMapOf()
        jsonDigisosSoker.hendelser.filterIsInstance<JsonSaksStatus>()
                .forEach { jsonHendelse -> saker[jsonHendelse.referanse] = jsonHendelse.tittel ?: "" }
        return saker
    }

    private fun mapToHendelseFrontend(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker, saker: MutableMap<String, String>): HendelseFrontend? {
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

    private fun tildeltNavKontorHendelse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker): HendelseFrontend? {
        jsonHendelse as JsonTildeltNavKontor
        if (jsonHendelse.navKontor == soknadsmottaker.enhetsnummer) {
            return null
        }
        val navKontorNavn = norgClient.hentNavEnhet(jsonHendelse.navKontor).navn
        val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt ved $navKontorNavn. Videresendingen vil ikke påvirke saksbehandlingstiden"
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, null, null, null)
    }

    private fun soknadsStatusHendelse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker): HendelseFrontend {
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

        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, null, null, null)
    }

    private fun vedtakFattetHendelse(jsonHendelse: JsonHendelse, saker: MutableMap<String, String>): HendelseFrontend {
        jsonHendelse as JsonVedtakFattet
        if (jsonHendelse.utfall == null) {
            val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.vedtaksfil.referanse)
            return HendelseFrontend(jsonHendelse.hendelsestidspunkt, "Vedtak fattet", id, nr, refErTilSvarUt)
        }

        val utfall = jsonHendelse.utfall.utfall.value().toLowerCase().replace('_', ' ')
        val beskrivelse = if (jsonHendelse.referanse != null && saker.containsKey(jsonHendelse.referanse)
                && saker[jsonHendelse.referanse] != "") {
            "${saker[jsonHendelse.referanse]} er $utfall"
        } else {
            log.warn("Tilhørende SaksstatusHendelse manglet eller manglet tittel på saksstatus")
            "En sak har fått utfallet: $utfall"
        }
        val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.vedtaksfil.referanse)
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, id, nr, refErTilSvarUt)
    }

    private fun dokumentasjonEtterspurtHendelse(jsonHendelse: JsonHendelse): HendelseFrontend {
        jsonHendelse as JsonDokumentasjonEtterspurt
        val beskrivelse = "Du må laste opp mer dokumentasjon"
        val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.forvaltningsbrev.referanse)
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, id, nr, refErTilSvarUt)
    }

    private fun forelopigSvarHendelse(jsonHendelse: JsonHendelse): HendelseFrontend {
        jsonHendelse as JsonForelopigSvar
        val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"
        val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.forvaltningsbrev.referanse)
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, id, nr, refErTilSvarUt)
    }

    private fun saksStatusHendelse(jsonHendelse: JsonHendelse): HendelseFrontend? {
        jsonHendelse as JsonSaksStatus
        if (jsonHendelse.status == null) {
            return null
        } else if (jsonHendelse.tittel == null) {
            return HendelseFrontend(jsonHendelse.hendelsestidspunkt, "En sak har ikke innsyn", null, null, null)
        }

        val status = jsonHendelse.status.value().toLowerCase().replace('_', ' ')
        val beskrivelse = if (jsonHendelse.status == JsonSaksStatus.Status.IKKE_INNSYN) {
            "Saken ${jsonHendelse.tittel} har $status"
        } else {
            "Saken ${jsonHendelse.tittel} er $status"
        }
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, null, null, null)
    }

    private fun getReferanseInfo(referanse: JsonFilreferanse): Triple<Boolean, String, Int?> {
        val refErTilSvarUt = referanse.type == JsonFilreferanse.Type.SVARUT
        val id: String = getIdFromReferanse(referanse, refErTilSvarUt)
        val nr: Int? = getNrFromReferanse(referanse, refErTilSvarUt)
        return Triple(refErTilSvarUt, id, nr)
    }

    private fun getIdFromReferanse(referanse: JsonFilreferanse?, refErTilSvarUt: Boolean): String {
        return if (refErTilSvarUt) {
            referanse as JsonSvarUtFilreferanse
            referanse.id
        } else {
            referanse as JsonDokumentlagerFilreferanse
            referanse.id
        }
    }

    private fun getNrFromReferanse(referanse: JsonFilreferanse?, refErTilSvarUt: Boolean): Int? {
        return if (refErTilSvarUt) {
            referanse as JsonSvarUtFilreferanse
            referanse.nr
        } else {
            null
        }
    }
}