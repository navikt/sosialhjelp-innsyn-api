package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/innsyn/utbetalinger")
class UtbetalingerController2(
    private val tilgangskontroll: TilgangskontrollService,
    private val utbetalingerServiceNew: UtbetalingerService,
) {
    private val logger by logger()

    @GetMapping
    suspend fun hentUtbetalinger(): List<UtbetalingDto> {
        tilgangskontroll.sjekkTilgang()

        val utbetalingerPerSoknad = utbetalingerServiceNew.hentUtbetalinger()

        // Finn referanser som finnes på flere søknader
        val referanserMedFlereSoknader =
            utbetalingerPerSoknad
                .flatMap { (fiksDigisosId, utbetalinger) ->
                    utbetalinger.map { it.referanse to fiksDigisosId }
                }.groupBy({ it.first }, { it.second })
                .filter { it.value.distinct().size > 1 }
                .keys

        val flatUtbetalinger =
            utbetalingerPerSoknad
                .flatMap { (fiksDigisosId, utbetalinger) ->
                    utbetalinger.map {
                        it.toDto(
                            fiksDigisosId = fiksDigisosId,
                            knyttetTilFlereSoknader = it.referanse in referanserMedFlereSoknader,
                        )
                    }
                }.distinctBy { it.referanse } // Fjerner eventuelle duplikater basert på referanse

        return flatUtbetalinger
    }
}
