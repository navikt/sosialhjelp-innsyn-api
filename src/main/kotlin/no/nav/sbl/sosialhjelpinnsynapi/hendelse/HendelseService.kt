package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HendelseService(private val eventService: EventService,
                      private val vedleggService: VedleggService,
                      private val fiksClient: FiksClient) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val model = eventService.createModel(fiksDigisosId, token)
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token)

        val vedlegg = vedleggService.hentEttersendteVedlegg(digisosSak.ettersendtInfoNAV)
        leggTilHendelserForOpplastinger(model, vedlegg)

        val responseList = model.historikk.map { HendelseResponse(it.tidspunkt.toString(), it.tittel, it.url) }
        log.info("Hentet historikk for fiksDigisosId=$fiksDigisosId")
        return responseList
    }

    private fun leggTilHendelserForOpplastinger(model: InternalDigisosSoker, vedlegg: List<InternalVedlegg>) {
        val mottattHendelse = model.historikk.firstOrNull { it.tittel.contains("mottatt") }

        if (mottattHendelse != null) {
            vedlegg
                    .filter { it.tidspunktLastetOpp.isAfter(mottattHendelse.tidspunkt) }
                    .forEach { model.historikk.add(Hendelse("NAV har mottatt vedlegg fra deg", it.tidspunktLastetOpp)) }

            model.historikk.sortBy { it.tidspunkt }
        }
    }
}