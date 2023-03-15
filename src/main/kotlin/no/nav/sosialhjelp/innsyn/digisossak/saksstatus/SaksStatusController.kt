package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4])
@RestController
@RequestMapping("/api/v1/innsyn")
class SaksStatusController(
    private val saksStatusService: SaksStatusService,
    private val tilgangskontroll: Tilgangskontroll,
) {

    @GetMapping("/{fiksDigisosId}/saksStatus", produces = ["application/json;charset=UTF-8"])
    fun hentSaksStatuser(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<SaksStatusResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val saksStatuser = saksStatusService.hentSaksStatuser(fiksDigisosId, token)
        if (saksStatuser.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(saksStatuser)
    }
}
