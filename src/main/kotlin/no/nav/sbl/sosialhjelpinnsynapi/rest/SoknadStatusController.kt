package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadStatusResponse
import no.nav.sbl.sosialhjelpinnsynapi.soknadstatus.SoknadStatusService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn/")
class SoknadStatusController(private val soknadStatusService: SoknadStatusService) {

    @GetMapping("{fiksDigisosId}/soknadStatus")
    fun hentSoknadStatus(@PathVariable fiksDigisosId: String): ResponseEntity<SoknadStatusResponse> {
        // Gitt innlogget bruker
        val soknadStatus: SoknadStatusResponse = soknadStatusService.hentSoknadStatus(fiksDigisosId)
        return ResponseEntity.ok()
                .header("a", "b") // some headers
                .body(soknadStatus)
    }

}