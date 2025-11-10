package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/innsyn/utbetalinger")
class UtbetalingerController2(
    private val tilgangskontroll: TilgangskontrollService,
    private val utbetalingerServiceNew: UtbetalingerService,
) {
    @GetMapping
    suspend fun hentUtbetalinger(): List<UtbetalingDto> {
        tilgangskontroll.sjekkTilgang()

        return utbetalingerServiceNew.hentUtbetalinger().flatMap { (fiksDigisosId, utbetalinger) ->
            utbetalinger.map {
                it.toDto(
                    fiksDigisosId,
                )
            }
        }
    }
}
