package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sbl.sosialhjelpinnsynapi.domain.HendelseResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.hendelse.HendelseService
import no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class HendelseController(
        private val hendelseService: HendelseService,
        private val tilgangskontrollService: TilgangskontrollService
) {

    @GetMapping("/{fiksDigisosId}/hendelser", produces = ["application/json;charset=UTF-8"])
    fun hentHendelser(@PathVariable fiksDigisosId: String, @RequestHeader(value = AUTHORIZATION) token: String): ResponseEntity<List<HendelseResponse>> {
        tilgangskontrollService.harTilgang(getUserIdFromToken())

        val hendelser = hendelseService.hentHendelser(fiksDigisosId, token)
        return ResponseEntity.ok(hendelser)
    }
}
