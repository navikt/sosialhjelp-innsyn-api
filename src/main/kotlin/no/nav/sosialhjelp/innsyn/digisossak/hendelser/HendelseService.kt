package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import no.nav.sosialhjelp.innsyn.app.token.Token
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
    private val fiksClient: FiksClient,
) {
    suspend fun hentHendelseResponse(
        fiksDigisosId: String,
        token: Token,
    ): List<HendelseResponse> {
        val (hendelser, kommunenummer, enhetNummer, enhetNavn) = hentHendelser(fiksDigisosId, token)
        val responseList =
            hendelser.map {
                HendelseResponse(
                    it.tidspunkt.toString(),
                    it.hendelseType.name,
                    it.url,
                    it.tekstArgument,
                    it.saksReferanse,
                    enhetNummer,
                    enhetNavn,
                    kommunenummer,
                )
            }
        log.info("Hentet historikk med ${responseList.size} hendelser")
        return responseList
    }

    suspend fun hentHendelser(
        fiksDigisosId: String,
        token: Token,
    ): HendelseInfo {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId)
        val model = eventService.createModel(digisosSak)

        val vedlegg: List<InternalVedlegg> = vedleggService.hentEttersendteVedlegg(digisosSak, model)
        digisosSak.originalSoknadNAV?.timestampSendt?.let { model.leggTilHendelserForOpplastinger(it, vedlegg) }

        model.leggTilHendelserForUtbetalinger()
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

    private fun InternalDigisosSoker.leggTilHendelserForUtbetalinger() {
        utbetalinger
            .filter { it.status != UtbetalingsStatus.ANNULLERT }
            .groupBy { it.datoHendelse.rundNedTilNaermeste5Minutt() }
            .forEach { (_, grupperteVilkar) ->
                historikk.add(
                    Hendelse(HendelseTekstType.UTBETALINGER_OPPDATERT, grupperteVilkar[0].datoHendelse),
                )
            }
    }

    private fun LocalDateTime.rundNedTilNaermeste5Minutt(): LocalDateTime =
        withMinute((floor(this.minute / 5.0) * 5.0).toInt())
            .truncatedTo(ChronoUnit.MINUTES)

    companion object {
        private val log by logger()
    }
}
