package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.saksstatus.SaksStatusService
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
class SaksStatusController(
        private val saksStatusService: SaksStatusService,
        private val tilgangskontrollService: TilgangskontrollService
) {

    @GetMapping("/{fiksDigisosId}/saksStatus", produces = ["application/json;charset=UTF-8"])
    fun hentSaksStatuser(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<SaksStatusResponse>> {
        tilgangskontrollService.sjekkTilgang()

        val saksStatuser = saksStatusService.hentSaksStatuser(fiksDigisosId, token)
        if (saksStatuser.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(saksStatuser)
    }
}
