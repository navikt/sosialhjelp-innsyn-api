package no.nav.sosialhjelp.innsyn.saksoversikt

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.DokumentasjonkravResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.VilkarResponse
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
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
    private val fiksClient: FiksClient,
    private val eventService: EventService,
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: TilgangskontrollService,
    meterRegistry: MeterRegistry,
) {
    private val antallSakerCounter: Counter = meterRegistry.counter("sosialhjelp.innsyn.antall_saker")

    @GetMapping("/saker")
    suspend fun hentAlleSaker(): ResponseEntity<List<SaksListeResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val alleSaker =
            try {
                saksOversiktService.hentAlleSaker(token)
            } catch (e: FiksException) {
                return ResponseEntity.status(503).build()
            }

        antallSakerCounter.increment(alleSaker.size.toDouble())
        if (alleSaker.isEmpty()) {
            log.info("Fant ingen saker for bruker")
        } else {
            log.info("Hentet alle (${alleSaker.size}) søknader for bruker")
        }
        return ResponseEntity.ok(alleSaker)
    }

    @GetMapping("/sak/{fiksDigisosId}/detaljer")
    suspend fun getSaksDetaljer(
        @PathVariable fiksDigisosId: String,
    ): SaksDetaljerResponse {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val sak = fiksClient.hentDigisosSak(fiksDigisosId, token)
        val model = eventService.createSaksoversiktModel(sak, token)
        val oppgaver = hentNyeOppgaver(model, sak.fiksDigisosId, token)
        val antallOppgaver =
            oppgaver.sumOf { it.oppgaveElementer.size } +
                hentAntallNyeVilkarOgDokumentasjonkrav(model, sak.fiksDigisosId, token)
        val harDokumentasjonEtterspurt = oppgaver.sumOf { it.oppgaveElementer.size } > 0
        val vilkar = hentNyeVilkar(model, sak.fiksDigisosId, token)
        val harVilkar = vilkar.isNotEmpty()
        val dokkrav = hentNyeDokumentasjonkrav(model, sak.fiksDigisosId, token)
        val dokumentasjonkrav = dokkrav.sumOf { it.dokumentasjonkravElementer.size } > 0

        return SaksDetaljerResponse(
            sak.fiksDigisosId,
            hentNavn(model),
            model.status,
            antallOppgaver,
            harDokumentasjonEtterspurt,
            dokumentasjonkrav,
            harVilkar,
            model.forelopigSvar,
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
            (oppgaver.mapNotNull { it.innsendelsesfrist } + dokkrav.mapNotNull { it.frist }).min()
        )
    }

    private fun hentNavn(model: InternalDigisosSoker): String =
        model.saker.filter { SaksStatus.FEILREGISTRERT != it.saksStatus }.joinToString { it.tittel ?: DEFAULT_SAK_TITTEL }

    private suspend fun hentAntallNyeVilkarOgDokumentasjonkrav(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: Token,
    ): Int {
        // Alle vilkår og dokumentasjonskrav fjernes hvis alle utbetalinger har status utbetalt/annullert og er forbigått utbetalingsperioden med 21 dager
        val filterUtbetalinger =
            model.utbetalinger
                .filter { utbetaling ->
                    utbetaling.status == UtbetalingsStatus.UTBETALT || utbetaling.status == UtbetalingsStatus.ANNULLERT
                }.filter { utbetaling -> utbetaling.tom?.isBefore(LocalDate.now().minusDays(21)) ?: false }

        return when {
            model.utbetalinger.isNotEmpty() && model.utbetalinger.size == filterUtbetalinger.size -> 0
            else -> hentNyeVilkar(model, fiksDigisosId, token).size + hentNyeDokumentasjonkrav(model, fiksDigisosId, token).sumOf { it.dokumentasjonkravElementer.size }
        }
    }

    private suspend fun hentNyeOppgaver(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: Token,
    ): List<OppgaveResponse> =
        when {
            model.oppgaver.isEmpty() -> emptyList()
            else -> oppgaveService.hentOppgaver(fiksDigisosId, token)
        }

    private suspend fun hentNyeVilkar(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: Token,
    ): List<VilkarResponse> =
        when {
            model.vilkar.isEmpty() -> emptyList()
            else -> oppgaveService.getVilkar(fiksDigisosId, token)
        }

    private suspend fun hentNyeDokumentasjonkrav(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: Token,
    ): List<DokumentasjonkravResponse> =
        when {
            model.dokumentasjonkrav.isEmpty() -> emptyList()
            else -> oppgaveService.getDokumentasjonkrav(fiksDigisosId, token)
        }

    companion object {
        private val log by logger()
    }
}
