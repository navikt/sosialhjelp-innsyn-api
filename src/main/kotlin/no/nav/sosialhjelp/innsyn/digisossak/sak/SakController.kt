package no.nav.sosialhjelp.innsyn.digisossak.sak

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class SakController(
    private val sakService: SakService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/sak/{vedtakId}")
    suspend fun hentSakForVedtak(
        @PathVariable fiksDigisosId: String,
        @PathVariable vedtakId: String,
    ): SakResponse {
        tilgangskontroll.sjekkTilgang()

        return sakService.hentSakForVedtak(fiksDigisosId, vedtakId)
    }
}
