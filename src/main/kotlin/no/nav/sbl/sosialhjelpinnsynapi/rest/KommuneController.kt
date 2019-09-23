package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn/kommune")
class KommuneController(private val kommuneService: KommuneService) {

    @GetMapping("/{kommunenummer}")
    fun hentKommuneInfo(@PathVariable kommunenummer: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<String>{
        val kommuneStatus = kommuneService.hentKommuneStatus(kommunenummer, token)

        return ResponseEntity.ok(kommuneStatus.toString())
    }

}
