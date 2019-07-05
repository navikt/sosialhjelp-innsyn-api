package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.oppgave.OppgaveService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class OppgaveController(val oppgaveService: OppgaveService) {

    @GetMapping("/{fiksDigisosId}/oppgaver", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getOppgaverForSoknad(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<OppgaveResponse>> {
        try {
            val oppgaverForSoknad = oppgaveService.getOppgaverForSoknad(fiksDigisosId, token)
            return ResponseEntity.ok(oppgaverForSoknad)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}