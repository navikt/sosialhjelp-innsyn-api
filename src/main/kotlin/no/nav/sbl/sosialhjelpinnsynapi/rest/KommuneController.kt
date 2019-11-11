package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneService
import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneStatusDetaljer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn/kommune")
class KommuneController(private val kommuneService: KommuneService) {

    @GetMapping("/{fiksDigisosId}")
    fun hentKommuneInfo(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<String>{
        val kommuneStatus = kommuneService.hentKommuneStatus(fiksDigisosId, token)

        return ResponseEntity.ok(kommuneStatus.toString())
    }

    @GetMapping()
    fun hentAlleKommuneStatuser() : ResponseEntity<List<KommuneStatusDetaljer>> {
        val alleKommunerMedStatus = kommuneService.hentAlleKommunerMedStatusStatus()
        return ResponseEntity.ok(alleKommunerMedStatus)
    }
}
