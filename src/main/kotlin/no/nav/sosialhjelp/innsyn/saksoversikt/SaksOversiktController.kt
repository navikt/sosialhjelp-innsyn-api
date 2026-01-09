package no.nav.sosialhjelp.innsyn.saksoversikt

import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.DokumentasjonkravResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.VilkarResponse
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/innsyn")
class SaksOversiktController(
    private val saksOversiktService: SaksOversiktService,
    private val fiksService: FiksService,
    private val eventService: EventService,
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/saker")
    suspend fun hentAlleSaker(): ResponseEntity<List<SaksListeResponse>> {
        tilgangskontroll.sjekkTilgang()

        val alleSaker =
            try {
                saksOversiktService.hentAlleSaker()
            } catch (e: FiksException) {
                return ResponseEntity.status(503).build()
            }

        return ResponseEntity.ok(alleSaker)
    }

    @GetMapping("/sak/{fiksDigisosId}/detaljer")
    suspend fun getSaksDetaljer(
        @PathVariable fiksDigisosId: String,
    ): SaksDetaljerResponse {
        tilgangskontroll.sjekkTilgang()

        val sak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createSaksoversiktModel(sak)
        val oppgaver = hentNyeOppgaver(model, sak.fiksDigisosId)
        val vilkar = hentNyeVilkar(model, sak.fiksDigisosId)
        val dokkrav = hentNyeDokumentasjonkrav(model, sak.fiksDigisosId)
        val mottattTidspunkt =
            model.historikk
                .firstOrNull {
                    it.hendelseType in
                        listOf(
                            HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN,
                            HendelseTekstType.SOKNAD_MOTTATT_UTEN_KOMMUNENAVN,
                        )
                }?.tidspunkt

        return SaksDetaljerResponse(
            fiksDigisosId = sak.fiksDigisosId,
            soknadTittel = hentNavn(model),
            status = model.status,
            antallNyeOppgaver =
                oppgaver.sumOf { it.oppgaveElementer.size } +
                    hentAntallNyeVilkarOgDokumentasjonkrav(model, sak.fiksDigisosId),
            dokumentasjonEtterspurt = oppgaver.sumOf { it.oppgaveElementer.size } > 0,
            dokumentasjonkrav = dokkrav.sumOf { it.dokumentasjonkravElementer.size } > 0,
            vilkar = vilkar.isNotEmpty(),
            forelopigSvar = model.forelopigSvar,
            saker =
                model.saker.map { sak ->
                    SaksDetaljerResponse.Sak(
                        sak.vedtak.size,
                        if (sak.vedtak.isEmpty()) {
                            sak.saksStatus ?: SaksStatus.UNDER_BEHANDLING
                        } else {
                            SaksStatus.FERDIGBEHANDLET
                        },
                    )
                },
            forsteOppgaveFrist = (oppgaver.mapNotNull { it.innsendelsesfrist } + dokkrav.mapNotNull { it.frist }).minOrNull(),
            sisteDokumentasjonKravFrist = dokkrav.mapNotNull { it.frist }.minOrNull(),
            mottattTidspunkt = mottattTidspunkt,
        )
    }

    private fun hentNavn(model: InternalDigisosSoker): String =
        model.saker.filter { SaksStatus.FEILREGISTRERT != it.saksStatus }.joinToString { it.tittel ?: DEFAULT_SAK_TITTEL }

    private suspend fun hentAntallNyeVilkarOgDokumentasjonkrav(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
    ): Int {
        // Alle vilkår og dokumentasjonskrav fjernes hvis alle utbetalinger har status utbetalt/annullert og er forbigått utbetalingsperioden med 21 dager
        val filterUtbetalinger =
            model.utbetalinger
                .filter { utbetaling ->
                    utbetaling.status == UtbetalingsStatus.UTBETALT || utbetaling.status == UtbetalingsStatus.ANNULLERT
                }.filter { utbetaling -> utbetaling.tom?.isBefore(LocalDate.now().minusDays(21)) ?: false }

        return when {
            model.utbetalinger.isNotEmpty() && model.utbetalinger.size == filterUtbetalinger.size -> 0
            else ->
                hentNyeVilkar(model, fiksDigisosId).size +
                    hentNyeDokumentasjonkrav(model, fiksDigisosId).sumOf { it.dokumentasjonkravElementer.size }
        }
    }

    private suspend fun hentNyeOppgaver(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
    ): List<OppgaveResponse> =
        when {
            model.oppgaver.isEmpty() -> emptyList()
            else -> oppgaveService.hentOppgaver(fiksDigisosId)
        }

    private suspend fun hentNyeVilkar(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
    ): List<VilkarResponse> =
        when {
            model.vilkar.isEmpty() -> emptyList()
            else -> oppgaveService.getVilkar(fiksDigisosId)
        }

    private suspend fun hentNyeDokumentasjonkrav(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
    ): List<DokumentasjonkravResponse> =
        when {
            model.dokumentasjonkrav.isEmpty() -> emptyList()
            else -> oppgaveService.getDokumentasjonkrav(fiksDigisosId)
        }
}
