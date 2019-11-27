package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.innsynOrginalSoknad.InnsynOrginalSoknadService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class InnsynOrginalSoknadController(private val innsynOrginalSoknadService: InnsynOrginalSoknadService) {

    @GetMapping("/{fiksDigisosId}/orginalSoknad")
    fun hentOrginalSoknad(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalSoknadResponse> {

        val orginalSoknadResponse: OrginalSoknadResponse = innsynOrginalSoknadService.hentOrginalSoknad(fiksDigisosId, token);

        return ResponseEntity.ok().body(orginalSoknadResponse);
    }
}