package no.nav.sosialhjelp.innsyn.tilgang

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.app.exceptions.PdlException
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@RestController
@RequestMapping("/api/v1/innsyn")
class TilgangController(
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/tilgang")
    fun harTilgang(
        @RequestHeader(value = AUTHORIZATION) token: String,
    ): ResponseEntity<TilgangResponse> {
        return try {
            val tilgang = tilgangskontroll.hentTilgang(getUserIdFromToken(), token)
            ResponseEntity.ok().body(TilgangResponse(tilgang.harTilgang, tilgang.fornavn))
        } catch (e: PdlException) {
            log.warn("Pdl kastet feil, returnerer 'harTilgang=true'")
            ResponseEntity.ok().body(TilgangResponse(true, ""))
        }
    }

    data class TilgangResponse(
        val harTilgang: Boolean,
        val fornavn: String,
    )

    companion object {
        private val log by logger()
    }
}
