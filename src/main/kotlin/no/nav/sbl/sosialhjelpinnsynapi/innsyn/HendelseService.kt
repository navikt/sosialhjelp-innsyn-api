package no.nav.sbl.sosialhjelpinnsynapi.innsyn

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonHendelse
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.*
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
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
        hendelser.add(HendelseFrontend(timestampSendt, "Søknaden med vedlegg er sendt til " + jsonSoknad.mottaker.navEnhetsnavn))
        hendelser.addAll(jsonDigisosSoker.hendelser.map { mapToHendelseFrontend(it) })
        hendelser.sortBy { it.timestamp }
        return hendelser
    }

    fun mapToHendelseFrontend(jsonHendelse: JsonHendelse): HendelseFrontend {
        if (jsonHendelse.type == null) {
            throw RuntimeException("Hendelse mangler type")
        }
        when (jsonHendelse.type) {
            JsonHendelse.Type.TILDELT_NAV_KONTOR ->
                return HendelseFrontend(jsonHendelse.hendelsestidspunkt, (jsonHendelse as JsonTildeltNavKontor).navKontor)
            JsonHendelse.Type.SOKNADS_STATUS ->
                return HendelseFrontend(jsonHendelse.hendelsestidspunkt, (jsonHendelse as JsonSoknadsStatus).status.value())
            JsonHendelse.Type.VEDTAK_FATTET ->
                return HendelseFrontend(jsonHendelse.hendelsestidspunkt, (jsonHendelse as JsonVedtakFattet).utfall.utfall.value())
            JsonHendelse.Type.DOKUMENTASJON_ETTERSPURT ->
                return HendelseFrontend(jsonHendelse.hendelsestidspunkt, "Dokumenter er etterspurt. Antall: " + (jsonHendelse as JsonDokumentasjonEtterspurt).dokumenter.size.toString())
            JsonHendelse.Type.FORELOPIG_SVAR ->
                return HendelseFrontend(jsonHendelse.hendelsestidspunkt, (jsonHendelse as JsonForelopigSvar).forvaltningsbrev.toString())
            JsonHendelse.Type.SAKS_STATUS ->
                return HendelseFrontend(jsonHendelse.hendelsestidspunkt, (jsonHendelse as JsonSaksStatus).status.value())
        }
    }
}