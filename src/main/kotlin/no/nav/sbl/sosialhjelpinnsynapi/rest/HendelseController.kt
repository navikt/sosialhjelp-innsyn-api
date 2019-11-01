package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.hendelse.HendelseService
import no.nav.security.oidc.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class HendelseController(val hendelseService: HendelseService) {

    @GetMapping("/{fiksDigisosId}/hendelser", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentHendelser(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<HendelseResponse>> {
        val hendelser = hendelseService.hentHendelser(fiksDigisosId, token)
        return ResponseEntity.ok(hendelser)
    }
}