package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.domain.OrginalJsonSoknadResponse
import no.nav.sosialhjelp.innsyn.domain.OrginalSoknadPdfLinkResponse
import no.nav.sosialhjelp.innsyn.service.originalsoknad.OrginalSoknadService
import no.nav.sosialhjelp.innsyn.service.tilgangskontroll.Tilgangskontroll
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
class OrginalSoknadController(
    private val orginalSoknadService: OrginalSoknadService,
    private val tilgangskontroll: Tilgangskontroll
) {

    @GetMapping("/{fiksDigisosId}/orginalJsonSoknad")
    fun getOrginalJsonSoknad(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalJsonSoknadResponse> {
        tilgangskontroll.sjekkTilgang(token)

        val orginalSoknadResponse: OrginalJsonSoknadResponse = orginalSoknadService.hentOrginalJsonSoknad(fiksDigisosId, token)
            ?: return ResponseEntity(HttpStatus.NO_CONTENT)

        return ResponseEntity.ok().body(orginalSoknadResponse)
    }

    @GetMapping("/{fiksDigisosId}/orginalSoknadPdfLink")
    fun getOrginalSoknadPdfLink(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalSoknadPdfLinkResponse> {
        tilgangskontroll.sjekkTilgang(token)

        val orginalSoknadPdfLink = (
            orginalSoknadService.hentOrginalSoknadPdfLink(fiksDigisosId, token)
                ?: return ResponseEntity(HttpStatus.NO_CONTENT)
            )

        return ResponseEntity.ok().body(orginalSoknadPdfLink)
    }
}
