package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class HendelseController(
    private val hendelseService: HendelseService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/hendelser", produces = ["application/json;charset=UTF-8"])
    suspend fun hentHendelser(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<HendelseResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val hendelser = hendelseService.hentHendelser(fiksDigisosId, token)
        return ResponseEntity.ok(hendelser)
    }
}
