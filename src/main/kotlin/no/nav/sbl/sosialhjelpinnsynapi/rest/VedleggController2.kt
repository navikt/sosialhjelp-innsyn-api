package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// todo: slå sammen med VedleggController fra annen branch

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController2(private val vedleggService: VedleggService) {

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun hentVedlegg(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<VedleggResponse>> {
        val vedleggResponses: List<VedleggResponse> = vedleggService.hentAlleVedlegg(fiksDigisosId)
        if (vedleggResponses.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        return ResponseEntity.ok(vedleggResponses)
    }
}