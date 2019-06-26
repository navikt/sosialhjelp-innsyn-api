package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.SaksStatusService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class SaksStatusController(private val saksStatusService: SaksStatusService) {

    @GetMapping("/{fiksDigisosId}/saksStatus", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getInfoOmSplittetSak(@PathVariable fiksDigisosId: String): ResponseEntity<List<SaksStatusResponse>> {
        try {
            saksStatusService.hentSaksStatuser(fiksDigisosId)
            return ResponseEntity.ok(emptyList())
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }
}