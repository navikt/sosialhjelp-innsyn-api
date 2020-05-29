package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalJsonSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalSoknadPdfLinkResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.originalsoknad.OrginalSoknadService
import no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
        private val tilgangskontrollService: TilgangskontrollService
) {

    @GetMapping("/{fiksDigisosId}/orginalJsonSoknad")
    fun getOrginalJsonSoknad(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalJsonSoknadResponse> {
        tilgangskontrollService.sjekkTilgang(getUserIdFromToken())

        val orginalSoknadResponse: OrginalJsonSoknadResponse = orginalSoknadService.hentOrginalJsonSoknad(fiksDigisosId, token)
                ?: return ResponseEntity(HttpStatus.NO_CONTENT)

        return ResponseEntity.ok().body(orginalSoknadResponse)
    }

    @GetMapping("/{fiksDigisosId}/orginalSoknadPdfLink")
    fun getOrginalSoknadPdfLink(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<OrginalSoknadPdfLinkResponse> {
        tilgangskontrollService.sjekkTilgang(getUserIdFromToken())

        val orginalSoknadPdfLink = (orginalSoknadService.hentOrginalSoknadPdfLink(fiksDigisosId, token)
                ?: return ResponseEntity(HttpStatus.NO_CONTENT))

        return ResponseEntity.ok().body(orginalSoknadPdfLink)
    }
}
