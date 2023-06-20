package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import no.finn.unleash.Unleash
import no.nav.sosialhjelp.innsyn.app.featuretoggle.VILKAR_ENABLED
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
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

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(digisosSak, model, token)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { model.leggTilHendelserForOpplastinger(it, vedlegg) }

        model.leggTilHendelserForUtbetalinger()

        if (unleashClient.isEnabled(VILKAR_ENABLED, false)) {
            model.leggTilHendelserForVilkar()
        }

        val responseList = model.historikk
            .sortedBy { it.tidspunkt }
            .map { HendelseResponse(it.tidspunkt.toString(), it.hendelseType.name, it.url, it.tekstArgument) }
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
                    Hendelse(HendelseTekstType.ANTALL_SENDTE_VEDLEGG, tidspunkt, tekstArgument = "$antallVedleggForTidspunkt")
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
                    Hendelse(HendelseTekstType.VILKAR_OPPDATERT, grupperteVilkar[0].datoSistEndret)
                )
            }
    }

    private fun InternalDigisosSoker.leggTilHendelserForUtbetalinger() {
        utbetalinger
            .filter { it.status != UtbetalingsStatus.ANNULLERT }
            .groupBy { it.datoHendelse.rundNedTilNaermeste5Minutt() }
            .forEach { (_, grupperteVilkar) ->
                historikk.add(
                    Hendelse(HendelseTekstType.UTBETALINGER_OPPDATERT, grupperteVilkar[0].datoHendelse)
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
