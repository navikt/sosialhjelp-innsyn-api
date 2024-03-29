package no.nav.sosialhjelp.innsyn.saksoversikt

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class SaksOversiktController(
    private val saksOversiktService: SaksOversiktService,
    private val fiksClient: FiksClient,
    private val eventService: EventService,
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/saker")
    fun hentAlleSaker(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): ResponseEntity<List<SaksListeResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)
                val alleSaker =
                    try {
                        saksOversiktService.hentAlleSaker(token)
                    } catch (e: FiksException) {
                        return@withContext ResponseEntity.status(503).build()
                    }

                log.info("Hentet alle (${alleSaker.size}) søknader for bruker, fra DigisosApi og fra SvarUt (via soknad-api).")
                ResponseEntity.ok().body(alleSaker)
            }
        }

    @GetMapping("/saksDetaljer")
    fun hentSaksDetaljer(
        @RequestParam id: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): ResponseEntity<SaksDetaljerResponse> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)

                if (id.isEmpty()) {
                    return@withContext ResponseEntity.noContent().build()
                }
                val sak = fiksClient.hentDigisosSak(id, token, true)
                val model = eventService.createSaksoversiktModel(sak, token)
                val antallOppgaver =
                    hentAntallNyeOppgaver(model, sak.fiksDigisosId, token) +
                        hentAntallNyeVilkarOgDokumentasjonkrav(model, sak.fiksDigisosId, token)
                val saksDetaljerResponse =
                    SaksDetaljerResponse(
                        sak.fiksDigisosId,
                        hentNavn(model),
                        model.status.name,
                        antallOppgaver,
                    )
                ResponseEntity.ok().body(saksDetaljerResponse)
            }
        }

    private fun hentNavn(model: InternalDigisosSoker): String {
        return model.saker.filter { SaksStatus.FEILREGISTRERT != it.saksStatus }.joinToString {
            it.tittel ?: DEFAULT_SAK_TITTEL
        }
    }

    private suspend fun hentAntallNyeVilkarOgDokumentasjonkrav(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: String,
    ): Int {
        // Alle vilkår og dokumentasjonskrav fjernes hvis alle utbetalinger har status utbetalt/annullert og er forbigått utbetalingsperioden med 21 dager
        val filterUtbetalinger =
            model.utbetalinger
                .filter { utbetaling ->
                    utbetaling.status == UtbetalingsStatus.UTBETALT || utbetaling.status == UtbetalingsStatus.ANNULLERT
                }
                .filter { utbetaling -> utbetaling.tom?.isBefore(LocalDate.now().minusDays(21)) ?: false }

        return when {
            model.utbetalinger.size > 0 && model.utbetalinger.size == filterUtbetalinger.size -> 0
            else -> hentAntallNyeVilkar(model, fiksDigisosId, token) + hentAntallNyeDokumentasjonkrav(model, fiksDigisosId, token)
        }
    }

    private suspend fun hentAntallNyeOppgaver(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: String,
    ): Int {
        return when {
            model.oppgaver.isEmpty() -> 0
            else -> oppgaveService.hentOppgaver(fiksDigisosId, token).sumOf { it.oppgaveElementer.size }
        }
    }

    private suspend fun hentAntallNyeVilkar(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: String,
    ): Int {
        return when {
            model.vilkar.isEmpty() -> 0
            else -> oppgaveService.getVilkar(fiksDigisosId, token).size
        }
    }

    private suspend fun hentAntallNyeDokumentasjonkrav(
        model: InternalDigisosSoker,
        fiksDigisosId: String,
        token: String,
    ): Int {
        return when {
            model.dokumentasjonkrav.isEmpty() -> 0
            else -> oppgaveService.getDokumentasjonkrav(fiksDigisosId, token).sumOf { it.dokumentasjonkravElementer.size }
        }
    }

    companion object {
        private val log by logger()
    }
}
