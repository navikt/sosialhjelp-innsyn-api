package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonDokumentlagerFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.filreferanse.JsonSvarUtFilreferanse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknadsmottaker
import no.nav.sbl.sosialhjelpinnsynapi.rest.HendelseFrontend
import org.springframework.stereotype.Component

@Component
class HendelseService(private val innsynService: InnsynService) {

    fun getHendelserForSoknad(fiksDigisosId: String): List<HendelseFrontend> {
        val jsonDigisosSoker = innsynService.hentDigisosSak(fiksDigisosId)
        val jsonSoknad = innsynService.hentOriginalSoknad(fiksDigisosId)
        val timestampSendt = innsynService.hentInnsendingstidspunktForOriginalSoknad(fiksDigisosId).toString()
        return createHendelserList(jsonDigisosSoker, jsonSoknad, timestampSendt)
    }

    private fun createHendelserList(jsonDigisosSoker: JsonDigisosSoker, jsonSoknad: JsonSoknad, timestampSendt: String): List<HendelseFrontend> {
        val hendelser = mutableListOf<HendelseFrontend>()
        val soknadsmottaker = jsonSoknad.mottaker
        val saker: MutableMap<String, String> = mapSaksStatusHendelserToMapOfReferanser(jsonDigisosSoker)
        hendelser.add(HendelseFrontend(timestampSendt, "Søknaden med vedlegg er sendt til " + soknadsmottaker.navEnhetsnavn, null, null, null))
        hendelser.addAll(jsonDigisosSoker.hendelser.map { mapToHendelseFrontend(it, soknadsmottaker, saker) }.filterNotNull())
        hendelser.sortBy { it.timestamp }
        return hendelser
    }

    private fun mapSaksStatusHendelserToMapOfReferanser(jsonDigisosSoker: JsonDigisosSoker): MutableMap<String, String> {
        val saker: MutableMap<String, String> = mutableMapOf()
        jsonDigisosSoker.hendelser.filter { jsonHendelse -> jsonHendelse.type == JsonHendelse.Type.SAKS_STATUS }
                .forEach { jsonHendelse -> saker.put((jsonHendelse as JsonSaksStatus).referanse, jsonHendelse.tittel) }
        return saker
    }

    private fun mapToHendelseFrontend(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker, saker: MutableMap<String, String>): HendelseFrontend? {
        if (jsonHendelse.type == null) {
            throw RuntimeException("Hendelse mangler type")
        }
        when (jsonHendelse.type) {
            JsonHendelse.Type.TILDELT_NAV_KONTOR -> return tildeltNavKontorHendelse(jsonHendelse, soknadsmottaker)
            JsonHendelse.Type.SOKNADS_STATUS -> return soknadsStatusHendelse(jsonHendelse, soknadsmottaker)
            JsonHendelse.Type.VEDTAK_FATTET -> return vedtakFattetHendelse(jsonHendelse, saker)
            JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT -> return DokumentasjonEtterspurtHendelse(jsonHendelse)
            JsonHendelse.Type.FORELOPIG_SVAR -> return ForelopigSvarHendelse(jsonHendelse)
            JsonHendelse.Type.SAKS_STATUS -> return SaksStatusHendelse(jsonHendelse)
        }
        throw RuntimeException("Hendelsestype" + jsonHendelse.type.value() + "mangler mapping")
    }

    private fun tildeltNavKontorHendelse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker): HendelseFrontend? {
        jsonHendelse as JsonTildeltNavKontor
        if (jsonHendelse.navKontor == soknadsmottaker.enhetsnummer) {
            return null
        }
        val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt hos " + jsonHendelse.navKontor
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, null, null, null)
    }

    private fun soknadsStatusHendelse(jsonHendelse: JsonHendelse, soknadsmottaker: JsonSoknadsmottaker): HendelseFrontend {
        jsonHendelse as JsonSoknadsStatus
        val beskrivelse: String
        if (jsonHendelse.status == null) {
            throw RuntimeException("JsonSoknadsStatus mangler status")
        }
        when (jsonHendelse.status) {
            JsonSoknadsStatus.Status.MOTTATT -> beskrivelse = "Søknaden med vedlegg er mottatt hos ${soknadsmottaker.navEnhetsnavn}"
            JsonSoknadsStatus.Status.UNDER_BEHANDLING -> beskrivelse = "Søknaden er under behandling"
            JsonSoknadsStatus.Status.FERDIGBEHANDLET -> beskrivelse = "Søknaden er ferdig behandlet"
        }

        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, null, null, null)
    }

    private fun vedtakFattetHendelse(jsonHendelse: JsonHendelse, saker: MutableMap<String, String>): HendelseFrontend {
        jsonHendelse as JsonVedtakFattet
        val utfall = jsonHendelse.utfall.utfall.value().toLowerCase().replace('_', ' ')
        val beskrivelse = saker[jsonHendelse.referanse] + " er " + utfall
        val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.vedtaksfil.referanse)
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, id, nr, refErTilSvarUt)
    }

    private fun DokumentasjonEtterspurtHendelse(jsonHendelse: JsonHendelse): HendelseFrontend {
        jsonHendelse as JsonDokumentasjonEtterspurt
        val beskrivelse = "Du må laste opp mer dokumentasjon"
        val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.forvaltningsbrev.referanse)
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, id, nr, refErTilSvarUt)
    }

    private fun ForelopigSvarHendelse(jsonHendelse: JsonHendelse): HendelseFrontend {
        jsonHendelse as JsonForelopigSvar
        val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"
        val (refErTilSvarUt, id: String, nr: Int?) = getReferanseInfo(jsonHendelse.forvaltningsbrev.referanse)
        return HendelseFrontend(jsonHendelse.hendelsestidspunkt, beskrivelse, id, nr, refErTilSvarUt)
    }

    private fun SaksStatusHendelse(jsonHendelse: JsonHendelse): HendelseFrontend {
        jsonHendelse as JsonSaksStatus
        val beskrivelse: String
        val status = jsonHendelse.status.value().toLowerCase().replace('_', ' ')
        if (jsonHendelse.status == JsonSaksStatus.Status.IKKE_INNSYN) {
            beskrivelse = "Saken " + jsonHendelse.tittel + " har " + status
        } else {
            beskrivelse = "Saken " + jsonHendelse.tittel + " er " + status
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