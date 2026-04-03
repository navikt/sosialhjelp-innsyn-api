package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.asFlux
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/v2/innsyn/utbetalinger")
class UtbetalingerController2(
    private val tilgangskontroll: TilgangskontrollService,
    private val utbetalingerServiceNew: UtbetalingerService,
) {
    private val logger by logger()

    @GetMapping
    suspend fun hentUtbetalinger(): Flux<ServerSentEvent<UtbetalingDto>> {
        tilgangskontroll.sjekkTilgang()

        val utbetalingerPerSoknad = utbetalingerServiceNew.hentUtbetalinger()

        return utbetalingerPerSoknad.findSoknaderForPayment().map { ServerSentEvent.builder(it).build() }.asFlux()
    }

    // Finn alle søknader (fiksDigisosId) per utbetalingsreferanse
    @OptIn(ExperimentalCoroutinesApi::class)
    @WithSpan
    private fun Flow<Soknad>.findSoknaderForPayment(): Flow<UtbetalingDto> =
        flatMapConcat {
            it.utbetalinger
                .map { utbetaling ->
                    utbetaling.toDto(it.fiksDigisosId, emptyList())
                }.asFlow()
        }
}
