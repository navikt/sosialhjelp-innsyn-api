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

        val flatUtbetalinger =
            utbetalingerServiceNew
                .hentUtbetalinger()
                .flatMap { (fiksDigisosId, utbetalinger) ->
                    utbetalinger.map {
                        it.toDto(fiksDigisosId)
                    }
                }.also { utbetalinger ->
                    // Grunnet rapportert feil med at bruker ser duplikate utbetalinger, legger vi til egen logging på dette.
                    // Kan fjernes når vi er sikre på at problemet er løst.
                    val duplikater =
                        utbetalinger
                            .groupBy { it.referanse }
                            .filter { it.value.size > 1 }
                    if (duplikater.isNotEmpty()) {
                        val duplikatLogg =
                            duplikater
                                .flatMap { (ref, list) ->
                                    list.map { dto -> "fiksDigisosId=${dto.fiksDigisosId}, referanse=$ref" }
                                }.joinToString(", ")
                        logger.warn("Fant duplikate utbetalinger: $duplikatLogg")
                    }
                }.distinctBy { it.referanse } // Fjerner eventuelle duplikater basert på referanse

        return flatUtbetalinger
    }
}
