package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.innsyn.OppgaveService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class OppgaveController(val oppgaveService: OppgaveService) {

    @GetMapping("/{fiksDigisosId}/oppgaver", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getOppgaverForSoknad(@PathVariable fiksDigisosId: String): ResponseEntity<List<OppgaveFrontend>> {
        try {
            val oppgaverForSoknad = oppgaveService.getOppgaverForSoknad(fiksDigisosId)
            return ResponseEntity.ok(oppgaverForSoknad)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}

data class OppgaveFrontend(
        val innsendelsesfrist: String,
        val dokumenttype: String,
        val tilleggsinformasjon: String?
)