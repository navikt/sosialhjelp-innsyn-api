package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.soknadstatus.SoknadStatusService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadStatusController(private val soknadStatusService: SoknadStatusService) {

    @GetMapping("{fiksDigisosId}/soknadStatus")
    fun hentSoknadStatus(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<SoknadStatusResponse> {
        // Gitt innlogget bruker
        val soknadStatus: SoknadStatusResponse = soknadStatusService.hentSoknadStatus(fiksDigisosId, token)
        return ResponseEntity.ok().body(soknadStatus)
    }

}