package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class OppgaveController(
        private val oppgaveService: OppgaveService,
        private val tilgangskontrollService: TilgangskontrollService
) {

    @GetMapping("/{fiksDigisosId}/oppgaver", produces = ["application/json;charset=UTF-8"])
    fun hentOppgaver(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<OppgaveResponse>> {
        tilgangskontrollService.sjekkTilgang()

        val oppgaver = oppgaveService.hentOppgaver(fiksDigisosId, token)
        if (oppgaver.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(oppgaver)
    }

    @GetMapping("/{fiksDigisosId}/oppgaver/{oppgaveId}", produces = ["application/json;charset=UTF-8"])
    fun hentOppgaveMedId(@PathVariable fiksDigisosId: String, @PathVariable oppgaveId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<OppgaveResponse>> {
        tilgangskontrollService.sjekkTilgang()

        val oppgaver = oppgaveService.hentOppgaverMedOppgaveId(fiksDigisosId, token, oppgaveId)
        if (oppgaver.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(oppgaver)
    }
}
