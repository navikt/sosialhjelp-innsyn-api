package no.nav.sosialhjelp.innsyn.rest

import kotlinx.coroutines.runBlocking
import no.finn.unleash.Unleash
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.client.dialog.DialogClient
import no.nav.sosialhjelp.innsyn.client.dialog.DialogException
import no.nav.sosialhjelp.innsyn.client.dialog.DialogStatus
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.client.unleash.DIALOG_UNDERSOKELSE_GRUPPE_1
import no.nav.sosialhjelp.innsyn.client.unleash.DIALOG_UNDERSOKELSE_GRUPPE_2
import no.nav.sosialhjelp.innsyn.client.unleash.DIALOG_UNDERSOKELSE_GRUPPE_3
import no.nav.sosialhjelp.innsyn.client.unleash.DIALOG_UNDERSOKELSE_GRUPPE_4
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
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
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
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
    private val dialogClient: DialogClient,
    private val clientProperties: ClientProperties,
    private val unleashClient: Unleash,
) {

    @GetMapping("/saker")
    suspend fun hentAlleSaker(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<SaksListeResponse>> {
        tilgangskontroll.sjekkTilgang(token)

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
    suspend fun skalViseMeldingerLenke(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<Boolean> {
        tilgangskontroll.sjekkTilgang(token)

        try {
            val status = runBlocking {
                dialogClient.hentDialogStatus(SubjectHandlerUtils.getUserIdFromToken(), token.removePrefix(BEARER))
            }
            return ResponseEntity.ok().body(status.tilgangTilDialog)
        } catch (e: Exception) { // DialogException
            log.warn("Status kall mot dialog-api har feilet. Bruker gammel metode som backup.", e)
        }

        val saker = try {
            fiksClient.hentAlleDigisosSaker(token)
        } catch (e: FiksException) {
            return ResponseEntity.status(503).build()
        }

        val sisteSoknad = saker.sortedByDescending { it.originalSoknadNAV?.timestampSendt }.firstOrNull()

        return ResponseEntity.ok().body(sisteSoknad?.kommunenummer == clientProperties.meldingerKommunenummer)
    }

    @GetMapping("/dialogstatus")
    suspend fun hentDialogStatus(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<DialogStatus> {
        tilgangskontroll.sjekkTilgang(token)

        return try {
            val status = dialogClient.hentDialogStatus(SubjectHandlerUtils.getUserIdFromToken(), token.removePrefix(BEARER))
            ResponseEntity.ok().body(status)
        } catch (e: DialogException) {
            log.warn("Status kall mot dialog-api har feilet.", e)
            ResponseEntity.status(503).build()
        }
    }

    @GetMapping("/sisteSak")
    suspend fun hentSisteSak(@RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<DigisosSak> {
        tilgangskontroll.sjekkTilgang(token)

        val saker = try {
            fiksClient.hentAlleDigisosSaker(token)
        } catch (e: FiksException) {
            return ResponseEntity.status(503).build()
        }

        val sisteSoknad = saker.sortedByDescending { it.originalSoknadNAV?.timestampSendt }.firstOrNull()
            ?: return ResponseEntity.noContent().build()

        val lastDigit: Char = SubjectHandlerUtils.getUserIdFromToken()[10]

        if (unleashClient.isEnabled(DIALOG_UNDERSOKELSE_GRUPPE_1) && listOf('0', '1', '2').contains(lastDigit)) {
            return ResponseEntity.ok().body(sisteSoknad)
        }
        if (unleashClient.isEnabled(DIALOG_UNDERSOKELSE_GRUPPE_2) && listOf('3', '4').contains(lastDigit)) {
            return ResponseEntity.ok().body(sisteSoknad)
        }
        if (unleashClient.isEnabled(DIALOG_UNDERSOKELSE_GRUPPE_3) && listOf('5', '6', '7').contains(lastDigit)) {
            return ResponseEntity.ok().body(sisteSoknad)
        }
        if (unleashClient.isEnabled(DIALOG_UNDERSOKELSE_GRUPPE_4) && listOf('8', '9').contains(lastDigit)) {
            return ResponseEntity.ok().body(sisteSoknad)
        }

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/saksDetaljer")
    suspend fun hentSaksDetaljer(@RequestParam id: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<SaksDetaljerResponse> {
        tilgangskontroll.sjekkTilgang(token)

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
