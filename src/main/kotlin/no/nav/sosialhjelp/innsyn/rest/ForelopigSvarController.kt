package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvarResponse
import no.nav.sosialhjelp.innsyn.service.forelopigsvar.ForelopigSvarService
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
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
    private val forelopigSvarService: ForelopigSvarService,
    private val tilgangskontroll: Tilgangskontroll
) {
    @GetMapping("/{fiksDigisosId}/forelopigSvar")
    fun hentForelopigSvarStatus(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<ForelopigSvarResponse> {
        tilgangskontroll.sjekkTilgang(token)

        val forelopigSvarResponse: ForelopigSvarResponse = forelopigSvarService.hentForelopigSvar(fiksDigisosId, token)

        return ResponseEntity.ok().body(forelopigSvarResponse)
    }
}
