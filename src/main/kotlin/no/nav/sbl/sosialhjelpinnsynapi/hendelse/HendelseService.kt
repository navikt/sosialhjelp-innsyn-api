package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import no.nav.sbl.sosialhjelpinnsynapi.config.FeatureToggles
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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor


@Component
class HendelseService(private val eventService: EventService,
                      private val vedleggService: VedleggService,
                      private val fiksClient: FiksClient,
                      private val featureToggles: FeatureToggles) {

    companion object {
        val log by logger()
    }

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { model.leggTilHendelserForOpplastinger(it, vedlegg) }

        if (featureToggles.utbetalingerEnabled) {
            model.leggTilHendelserForVilkar()
            model.leggTilHendelserForUtbetalinger()
        }

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
                .flatMap { it.utbetalinger }
                .flatMap { it.vilkar }
                .groupBy { it.datoSistEndret.rundNedTilNaermeste5Minutt() }
                .forEach { (_, grupperteVilkar) ->
                    historikk.add(
                            Hendelse("Dine vilkår har blitt oppdatert, les vedtaket for mer detaljer.", grupperteVilkar[0].datoSistEndret)
                    )
                }
    }

    private fun InternalDigisosSoker.leggTilHendelserForUtbetalinger() {
        utbetalinger
//                .filterNot { it.status == UtbetalingsStatus.ANNULLERT } // Finn ut om annullert skal gi melding i historikk
                .groupBy { it.datoHendelse.rundNedTilNaermeste5Minutt() }
                .forEach { (_, grupperteVilkar) ->
                    historikk.add(
                            Hendelse("Utbetalingsplanen din har blitt oppdatert.", grupperteVilkar[0].datoHendelse) // TODO: lenke til utbetalingsplan
                    )
                }
    }

    private fun LocalDateTime.rundNedTilNaermeste5Minutt(): LocalDateTime {
        return withMinute((floor(this.minute / 5.0) * 5.0).toInt())
                .truncatedTo(ChronoUnit.MINUTES)
    }

}