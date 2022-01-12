package no.nav.sosialhjelp.innsyn.rest

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.common.PdlException
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.service.tilgangskontroll.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class TilgangController(
    private val tilgangskontroll: Tilgangskontroll
) {

    @GetMapping("/tilgang")
    fun harTilgang(@RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<TilgangResponse> {
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
        val fornavn: String
    )

    companion object {
        private val log by logger()
    }
}
