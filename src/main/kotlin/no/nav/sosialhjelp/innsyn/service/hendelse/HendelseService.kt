package no.nav.sosialhjelp.innsyn.service.hendelse

import no.finn.unleash.Unleash
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.client.unleash.VILKAR_ENABLED
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseResponse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor

@Component
class HendelseService(
    private val eventService: EventService,
    private val vedleggService: VedleggService,
    private val fiksClient: FiksClient,
    private val unleashClient: Unleash
) {

    fun hentHendelser(fiksDigisosId: String, token: String): List<HendelseResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(fiksDigisosId, digisosSak.ettersendtInfoNAV, token)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { model.leggTilHendelserForOpplastinger(it, vedlegg) }

        model.leggTilHendelserForUtbetalinger()

        if (unleashClient.isEnabled(VILKAR_ENABLED, false)) {
            model.leggTilHendelserForVilkar()
        }

        val responseList = model.historikk
            .sortedBy { it.tidspunkt }
            .map { HendelseResponse(it.tidspunkt.toString(), it.tittel, it.url) }
        log.info("Hentet historikk med ${responseList.size} hendelser")
        return responseList
    }

    private fun InternalDigisosSoker.leggTilHendelserForOpplastinger(timestampSoknadSendt: Long, vedlegg: List<InternalVedlegg>) {
        vedlegg
            .filter { it.tidspunktLastetOpp.isAfter(unixToLocalDateTime(timestampSoknadSendt)) }
            .filter { it.dokumentInfoList.isNotEmpty() }
            .groupBy { it.tidspunktLastetOpp }
            .forEach { (tidspunkt, samtidigOpplastedeVedlegg) ->
                val antallVedleggForTidspunkt = samtidigOpplastedeVedlegg.sumOf { it.dokumentInfoList.size }
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
                    Hendelse("Vilkårene dine er oppdatert, les vedtaket for mer detaljer.", grupperteVilkar[0].datoSistEndret)
                )
            }
    }

    private fun InternalDigisosSoker.leggTilHendelserForUtbetalinger() {
        utbetalinger
//                .filterNot { it.status == UtbetalingsStatus.ANNULLERT } // TODO - Finn ut om annullert skal gi melding i historikk
            .groupBy { it.datoHendelse.rundNedTilNaermeste5Minutt() }
            .forEach { (_, grupperteVilkar) ->
                historikk.add(
                    Hendelse("Dine utbetalinger har blitt oppdatert.", grupperteVilkar[0].datoHendelse)
                )
            }
    }

    private fun LocalDateTime.rundNedTilNaermeste5Minutt(): LocalDateTime {
        return withMinute((floor(this.minute / 5.0) * 5.0).toInt())
            .truncatedTo(ChronoUnit.MINUTES)
    }

    companion object {
        private val log by logger()
    }
}
