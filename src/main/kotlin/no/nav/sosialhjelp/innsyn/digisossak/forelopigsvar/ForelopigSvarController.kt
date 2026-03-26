package no.nav.sosialhjelp.innsyn.digisossak.forelopigsvar

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class ForelopigSvarController(
    private val forelopigSvarService: ForelopigSvarService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/forelopigSvar")
    suspend fun hentForelopigSvarStatus(
        @PathVariable fiksDigisosId: String,
    ): ForelopigSvarResponse {
        tilgangskontroll.sjekkTilgang()

        return forelopigSvarService.hentForelopigSvar(fiksDigisosId)
    }
}
