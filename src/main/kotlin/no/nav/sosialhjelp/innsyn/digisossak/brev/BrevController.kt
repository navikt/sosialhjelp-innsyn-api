package no.nav.sosialhjelp.innsyn.digisossak.brev

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/innsyn")
class BrevController(
    private val brevService: BrevService,
    private val tilgangskontrollService: TilgangskontrollService,
) {
    private val logger = LoggerFactory.getLogger(BrevController::class.java)

    @GetMapping("/{fiksDigisosId}/brev")
    suspend fun hentBrev(
        @PathVariable fiksDigisosId: String,
    ): List<Brev> {
        tilgangskontrollService.sjekkTilgang()
        return brevService.getBrev(fiksDigisosId)
    }
}
