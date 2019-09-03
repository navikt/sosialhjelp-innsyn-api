package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.SaksStatusService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class SaksStatusController(private val saksStatusService: SaksStatusService) {

    @GetMapping("/{fiksDigisosId}/saksStatus", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun hentSaksStatuser(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) access_token: String): ResponseEntity<List<SaksStatusResponse>> {
        try {
            val saksStatuser = saksStatusService.hentSaksStatuser(fiksDigisosId, access_token)
            if (saksStatuser.isEmpty()) {
                return ResponseEntity(HttpStatus.NO_CONTENT)
            }
            return ResponseEntity.ok(saksStatuser)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}