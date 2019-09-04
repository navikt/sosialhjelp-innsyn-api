package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggForHistorikkService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggForHistorikkService.Vedlegg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HendelseService(private val eventService: EventService,
                      private val vedleggForHistorikkService: VedleggForHistorikkService) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val model = eventService.createModel(fiksDigisosId)

        val vedlegg = vedleggForHistorikkService.hentVedlegg(fiksDigisosId)
        leggTilHendelserForOpplastingerEtterMottatt(model, vedlegg)

        val responseList = model.historikk.map { HendelseResponse(it.tidspunkt.toString(), it.tittel, it.url) }
        log.info("Hentet historikk for fiksDigisosId=$fiksDigisosId")
        return responseList
    }

    private fun leggTilHendelserForOpplastingerEtterMottatt(model: InternalDigisosSoker, vedlegg: List<Vedlegg>) {
        val mottattHendelse = model.historikk.firstOrNull { it.tittel.contains("mottatt") }

        if (mottattHendelse != null) {
            vedlegg
                    .filter { it.tidspunktLastetOpp.isAfter(mottattHendelse.tidspunkt) }
                    .forEach { model.historikk.add(Hendelse("Vedlegg lastet opp - ${it.type}", it.tidspunktLastetOpp)) }

            model.historikk.sortBy { it.tidspunkt }
        }
    }
}