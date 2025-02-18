package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class SaksStatusController(
    private val saksStatusService: SaksStatusService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/saksStatus", produces = ["application/json;charset=UTF-8"])
    suspend fun hentSaksStatuser(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<List<SaksStatusResponse>> {
        tilgangskontroll.sjekkTilgang(token)

        val saksStatuser = saksStatusService.hentSaksStatuser(fiksDigisosId, token)
        return if (saksStatuser.isEmpty()) {
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity.ok(saksStatuser)
        }
    }
}
