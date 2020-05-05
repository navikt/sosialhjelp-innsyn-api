package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.InternalVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.springframework.stereotype.Component


@Component
class HendelseService(
        private val eventService: EventService,
        private val vedleggService: VedleggService,
        private val fiksClient: FiksClient
) {

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { leggTilHendelserForOpplastinger(model, it, vedlegg) }

        val responseList = model.historikk.map { HendelseResponse(it.tidspunkt.toString(), it.tittel, it.url) }
        log.info("Hentet historikk med ${responseList.size} hendelser for digisosId=$fiksDigisosId")
        return responseList
    }

    private fun leggTilHendelserForOpplastinger(model: InternalDigisosSoker, timestampSoknadSendt: Long, vedlegg: List<InternalVedlegg>) {
        vedlegg
                .filter { it.tidspunktLastetOpp.isAfter(unixToLocalDateTime(timestampSoknadSendt)) }
                .filter { it.dokumentInfoList.isNotEmpty() }
                .groupBy { it.tidspunktLastetOpp }
                .forEach { (tidspunkt, samtidigOpplastedeVedlegg) ->
                    val antallVedleggForTidspunkt = samtidigOpplastedeVedlegg.sumBy { it.dokumentInfoList.size }
                    model.historikk.add(
                            Hendelse("Du har sendt $antallVedleggForTidspunkt vedlegg til NAV", tidspunkt)
                    )
                }
        model.historikk.sortBy { it.tidspunkt }
    }

    companion object {
        private val log by logger()
    }
}
