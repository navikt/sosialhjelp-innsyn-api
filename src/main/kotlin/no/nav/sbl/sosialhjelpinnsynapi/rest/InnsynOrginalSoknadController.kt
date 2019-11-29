package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalSoknadPdfLinkResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalJsonSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.innsynOrginalSoknad.InnsynOrginalSoknadService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class InnsynOrginalSoknadController(private val innsynOrginalSoknadService: InnsynOrginalSoknadService) {

    @GetMapping("/{fiksDigisosId}/orginalJsonSoknad")
    fun getOrginalJsonSoknad(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalJsonSoknadResponse> {

        val orginalSoknadResponse: OrginalJsonSoknadResponse = innsynOrginalSoknadService.hentOrginalJsonSoknad(fiksDigisosId, token)
                ?: return ResponseEntity(HttpStatus.NO_CONTENT);

        return ResponseEntity.ok().body(orginalSoknadResponse);
    }

    @GetMapping("/{fiksDigisosId}/orginalSoknadPdfLink")
    fun getOrginalSoknadPdfLink(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalSoknadPdfLinkResponse> {

        val orginalSoknadPdfLink = (innsynOrginalSoknadService.hentOrginalSoknadPdfLink(fiksDigisosId, token)
                ?: return ResponseEntity(HttpStatus.NO_CONTENT));

        return ResponseEntity.ok().body(orginalSoknadPdfLink);
    }
}
