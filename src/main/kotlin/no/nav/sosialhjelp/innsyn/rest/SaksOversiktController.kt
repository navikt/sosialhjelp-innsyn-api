package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SaksDetaljerResponse
import no.nav.sosialhjelp.innsyn.domain.SaksListeResponse
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.service.oppgave.OppgaveService
import no.nav.sosialhjelp.innsyn.service.saksstatus.DEFAULT_TITTEL
import no.nav.sosialhjelp.innsyn.service.tilgangskontroll.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixTimestampToDate
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class SaksOversiktController(
    private val fiksClient: FiksClient,
    private val eventService: EventService,
    private val oppgaveService: OppgaveService,
    private val tilgangskontroll: Tilgangskontroll,
    private val clientProperties: ClientProperties,
) {

    @GetMapping("/saker")
    fun hentAlleSaker(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<SaksListeResponse>> {
        tilgangskontroll.sjekkTilgang()

        val saker = try {
            fiksClient.hentAlleDigisosSaker(token)
        } catch (e: FiksException) {
            return ResponseEntity.status(503).build()
        }

        val responselist = saker
            .map {
                SaksListeResponse(
                    it.fiksDigisosId,
                    "Søknad om økonomisk sosialhjelp",
                    unixTimestampToDate(it.sistEndret),
                    IntegrationUtils.KILDE_INNSYN_API
                )
            }
        log.info("Hentet alle (${responselist.size}) DigisosSaker for bruker.")

        return ResponseEntity.ok().body(responselist.sortedByDescending { it.sistOppdatert })
    }

    @GetMapping("/skalViseMeldingerLenke")
    fun skalViseMeldingerLenke(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang()

        val saker = try {
            fiksClient.hentAlleDigisosSaker(token)
        } catch (e: FiksException) {
            return ResponseEntity.status(503).build()
        }

        val sisteSoknad = saker.sortedByDescending { it.originalSoknadNAV?.timestampSendt }.first()

        return ResponseEntity.ok().body(sisteSoknad.kommunenummer == clientProperties.meldingerKommunenummer)
    }

    @GetMapping("/saksDetaljer")
    fun hentSaksDetaljer(@RequestParam id: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<SaksDetaljerResponse> {
        tilgangskontroll.sjekkTilgang()

        if (id.isEmpty()) {
            return ResponseEntity.noContent().build()
        }
        val sak = fiksClient.hentDigisosSak(id, token, true)
        val model = eventService.createSaksoversiktModel(sak, token)
        val antallOppgaver = hentAntallNyeOppgaver(model, sak.fiksDigisosId, token) + hentAntallNyeVilkar(model, sak.fiksDigisosId, token) + hentAntallNyeDokumentasjonkrav(model, sak.fiksDigisosId, token)
        val saksDetaljerResponse = SaksDetaljerResponse(
            sak.fiksDigisosId,
            hentNavn(model),
            model.status?.let { mapStatus(it) } ?: "",
            antallOppgaver
        )
        return ResponseEntity.ok().body(saksDetaljerResponse)
    }

    private fun mapStatus(status: SoknadsStatus): String {
        return if (status == SoknadsStatus.BEHANDLES_IKKE) {
            SoknadsStatus.FERDIGBEHANDLET.name
        } else {
            status.name.replace('_', ' ')
        }
    }

    private fun hentNavn(model: InternalDigisosSoker): String {
        return model.saker.filter { SaksStatus.FEILREGISTRERT != it.saksStatus }.joinToString {
            it.tittel ?: DEFAULT_TITTEL
        }
    }

    private fun hentAntallNyeOppgaver(model: InternalDigisosSoker, fiksDigisosId: String, token: String): Int {
        return when {
            model.oppgaver.isEmpty() -> 0
            else -> oppgaveService.hentOppgaver(fiksDigisosId, token).sumOf { it.oppgaveElementer.size }
        }
    }
    private fun hentAntallNyeVilkar(model: InternalDigisosSoker, fiksDigisosId: String, token: String): Int {
        return when {
            model.vilkar.isEmpty() -> 0
            else -> oppgaveService.getVilkar(fiksDigisosId, token).size
        }
    }
    private fun hentAntallNyeDokumentasjonkrav(model: InternalDigisosSoker, fiksDigisosId: String, token: String): Int {
        return when {
            model.dokumentasjonkrav.isEmpty() -> 0
            else -> oppgaveService.getDokumentasjonkrav(fiksDigisosId, token).sumOf { it.dokumentasjonkravElementer.size }
        }
    }

    companion object {
        private val log by logger()
    }
}
