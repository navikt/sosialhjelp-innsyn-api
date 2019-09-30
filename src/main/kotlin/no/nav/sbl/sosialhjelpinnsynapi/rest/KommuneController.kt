package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.kommune.KommuneService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn/kommune")
class KommuneController(private val kommuneService: KommuneService) {

    @GetMapping("/{kommunenummer}")
    fun hentKommuneInfo(@PathVariable kommunenummer: String): ResponseEntity<String>{
        val kommuneStatus = kommuneService.hentKommuneStatus(kommunenummer)

        return ResponseEntity.ok(kommuneStatus.toString())
    }

}
