package no.nav.sosialhjelp.innsyn.rest

import no.finn.unleash.Unleash
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.client.unleash.DITTNAV_OPPGAVE_ENDEPUNKTER_ENABLED
import no.nav.sosialhjelp.innsyn.dittnav.DittNavOppgave
import no.nav.sosialhjelp.innsyn.dittnav.DittNavOppgaverService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", combineWithOr = true, claimMap = ["acr=Level3", "acr=Level4"])
@RestController
@RequestMapping("/api/dittnav")
class DittNavOppgaverController(
    private val dittNavOppgaverService: DittNavOppgaverService,
    private val unleash: Unleash,
) {

    @GetMapping("/oppgaver/aktive", produces = ["application/json;charset=UTF-8"])
    fun getAktiveOppgaver(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String
    ): ResponseEntity<List<DittNavOppgave>> {
        if (!unleash.isEnabled(DITTNAV_OPPGAVE_ENDEPUNKTER_ENABLED, false)) {
            return ResponseEntity.notFound().build()
        }
        // tilgangskontroll?

        val aktiveOppgaver = dittNavOppgaverService.hentAktiveOppgaver(token)
        return ResponseEntity.ok(aktiveOppgaver)
    }

    @GetMapping("/oppgaver/inaktive", produces = ["application/json;charset=UTF-8"])
    fun getInaktiveOppgaver(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String
    ): ResponseEntity<List<DittNavOppgave>> {
        if (!unleash.isEnabled(DITTNAV_OPPGAVE_ENDEPUNKTER_ENABLED, false)) {
            return ResponseEntity.notFound().build()
        }
        // tilgangskontroll?

        val inaktiveOppgaver = dittNavOppgaverService.hentInaktiveOppgaver(token)
        return ResponseEntity.ok(inaktiveOppgaver)
    }
}
