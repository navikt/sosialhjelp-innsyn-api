package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor

data class HendelseInfo(
    val hendelser: List<Hendelse>,
    val kommunenummer: String,
    val enhetNummer: String?,
    val enhetNavn: String?,
)

@Component
class HendelseService(
    private val eventService: EventService,
    private val vedleggService: VedleggService,
    private val fiksService: FiksService,
) {
    suspend fun hentHendelser(fiksDigisosId: String): HendelseInfo {
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(digisosSak, model)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { model.leggTilHendelserForOpplastinger(it, vedlegg) }

        model.grupperHendelserForUtbetalinger()
        return HendelseInfo(
            model.historikk.sortedBy { it.tidspunkt },
            digisosSak.kommunenummer,
            model.soknadsmottaker?.navEnhetsnummer,
            model.soknadsmottaker?.navEnhetsnavn,
        )
    }

    private fun InternalDigisosSoker.leggTilHendelserForOpplastinger(
        timestampSoknadSendt: Long,
        vedlegg: List<InternalVedlegg>,
    ) {
        vedlegg
            .filter { it.tidspunktLastetOpp.isAfter(unixToLocalDateTime(timestampSoknadSendt)) }
            .filter { it.dokumentInfoList.isNotEmpty() }
            .groupBy { it.tidspunktLastetOpp }
            .forEach { (tidspunkt, samtidigOpplastedeVedlegg) ->
                val antallVedleggForTidspunkt = samtidigOpplastedeVedlegg.sumOf { it.dokumentInfoList.size }
                historikk.add(
                    Hendelse(HendelseTekstType.ANTALL_SENDTE_VEDLEGG, tidspunkt, tekstArgument = "$antallVedleggForTidspunkt"),
                )
            }
    }

    /**
     * Slår sammen påfølgende utbetalingshendelser med samme status i samme 5-minuttersvindu.
     * Bare påfølgende, så planlagt → stoppet → planlagt gir tre linjer.
     */
    private fun InternalDigisosSoker.grupperHendelserForUtbetalinger() {
        val (utbetalingshendelser, andreHendelser) =
            historikk.partition { it.hendelseType == HendelseTekstType.UTBETALINGER_OPPDATERT }

        val grupperteUtbetalingshendelser =
            utbetalingshendelser
                .sortedBy { it.tidspunkt }
                .fold(mutableListOf<Hendelse>()) { grupperte, hendelse ->
                    val forrige = grupperte.lastOrNull()
                    val sammeGruppe =
                        forrige != null &&
                            forrige.utbetalingsStatus == hendelse.utbetalingsStatus &&
                            forrige.tidspunkt.rundNedTilNaermeste5Minutt() == hendelse.tidspunkt.rundNedTilNaermeste5Minutt()
                    if (!sammeGruppe) grupperte.add(hendelse)
                    grupperte
                }

        historikk = (andreHendelser + grupperteUtbetalingshendelser).toMutableList()
    }

    private fun LocalDateTime.rundNedTilNaermeste5Minutt(): LocalDateTime =
        withMinute((floor(this.minute / 5.0) * 5.0).toInt())
            .truncatedTo(ChronoUnit.MINUTES)

    companion object {
        private val log by logger()
    }
}
