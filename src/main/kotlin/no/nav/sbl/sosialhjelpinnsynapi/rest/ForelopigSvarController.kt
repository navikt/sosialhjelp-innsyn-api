package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvarResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.forelopigsvar.ForelopigSvarService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class ForelopigSvarController(
        val forelopigSvarService: ForelopigSvarService
) {
    @GetMapping("/{fiksDigisosId}/forelopigSvar")
    fun hentForelopigSvarStatus(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<ForelopigSvarResponse> {

        val forelopigSvarResponse: ForelopigSvarResponse = forelopigSvarService.hentForelopigSvar(fiksDigisosId, token)


        return ResponseEntity.ok().body(forelopigSvarResponse)
    }
}