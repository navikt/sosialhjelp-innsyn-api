package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.collections.component1
import kotlin.collections.component2

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

        return utbetalingerPerSoknad.findSoknaderForPayment()
    }

    // Finn alle søknader (fiksDigisosId) per utbetalingsreferanse
    @WithSpan
    private fun Map<String, List<Utbetaling>>.findSoknaderForPayment(): List<UtbetalingDto> {
        val soknaderPerReferanse =
            flatMap { (fiksDigisosId, utbetalinger) ->
                utbetalinger.map { it.referanse to fiksDigisosId }
            }.groupBy({ it.first }, { it.second })
                .mapValues { it.value.distinct() }

        val flatUtbetalinger =
            flatMap { (fiksDigisosId, utbetalinger) ->
                utbetalinger.map {
                    it.toDto(
                        fiksDigisosId = fiksDigisosId,
                        tilknyttedeSoknader = soknaderPerReferanse[it.referanse] ?: listOf(fiksDigisosId),
                    )
                }
            }.distinctBy { it.referanse } // Fjerner eventuelle duplikater basert på referanse

        return flatUtbetalinger
    }
}
