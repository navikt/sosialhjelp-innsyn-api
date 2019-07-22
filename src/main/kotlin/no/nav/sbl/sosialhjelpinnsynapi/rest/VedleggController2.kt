package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// todo: slå sammen med VedleggController fra annen branch

@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController2 (private val vedleggService: VedleggService) {

    @GetMapping("/{fiksDigisosId}/vedlegg")
    fun hentVedlegg(@PathVariable fiksDigisosId: String): ResponseEntity<List<VedleggResponse>> {
        val vedleggResponses: List<VedleggResponse> = vedleggService.execute(fiksDigisosId)
        if (vedleggResponses.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(vedleggResponses)
    }
}