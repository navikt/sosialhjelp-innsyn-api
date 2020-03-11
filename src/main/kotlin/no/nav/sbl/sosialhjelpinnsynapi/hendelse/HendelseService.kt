package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import org.springframework.stereotype.Component
import java.time.temporal.ChronoUnit
import kotlin.math.floor


@Component
class HendelseService(private val eventService: EventService,
                      private val vedleggService: VedleggService,
                      private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { model.leggTilHendelserForOpplastinger(it, vedlegg) }

        model.leggTilHendelserForVilkar()
        // model.leggTilHendelserForUtbetalingerOgRammevedtak()

        val responseList = model.historikk
                .sortedBy { it.tidspunkt }
                .map { HendelseResponse(it.tidspunkt.toString(), it.tittel, it.url) }
        log.info("Hentet historikk med ${responseList.size} hendelser for digisosId=$fiksDigisosId")
        return responseList
    }

    private fun InternalDigisosSoker.leggTilHendelserForOpplastinger(timestampSoknadSendt: Long, vedlegg: List<InternalVedlegg>) {
        vedlegg
                .filter { it.tidspunktLastetOpp.isAfter(unixToLocalDateTime(timestampSoknadSendt)) }
                .filter { it.dokumentInfoList.isNotEmpty() }
                .groupBy { it.tidspunktLastetOpp }
                .forEach { (tidspunkt, samtidigOpplastedeVedlegg) ->
                    val antallVedleggForTidspunkt = samtidigOpplastedeVedlegg.sumBy { it.dokumentInfoList.size }
                    historikk.add(
                            Hendelse("Du har sendt $antallVedleggForTidspunkt vedlegg til NAV", tidspunkt)
                    )
                }
    }

    private fun InternalDigisosSoker.leggTilHendelserForVilkar() {
        saker
                .flatMap { it.vilkar }
                .groupBy { it.datoSistEndret
                        .withMinute((floor(it.datoSistEndret.minute / 5.0) * 5.0).toInt()) // rund ned til nærmeste 5-minutt
                        .truncatedTo(ChronoUnit.MINUTES)
                }
                .forEach { (_, grupperteVilkar) ->
                    historikk.add(
                            Hendelse("Dine vilkår har blitt oppdatert, les vedtaket for mer detaljer", grupperteVilkar[0].datoSistEndret)
                    )
                }
    }

}