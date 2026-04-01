package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import io.getunleash.Unleash
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.event.apply
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Service

data class Soknad(
    val fiksDigisosId: String,
    val utbetalinger: List<Utbetaling>,
)

@Service("UtbetalingService2")
class UtbetalingerService(
    private val fiksService: FiksService,
    private val eventService: EventService,
    private val unleash: Unleash,
) {
    private val log by logger()

    suspend fun hentUtbetalingStreamingly() {
    }

    suspend fun hentUtbetalinger(): Flow<Soknad> {
        val digisosSaker = fiksService.getAllSoknader()
        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyFlow()
        }

        val newBulkApiEnabled = unleash.isEnabled("sosialhjelp.innsyn.fiks.bulk")
        return if (newBulkApiEnabled) {
            val soknader =
                digisosSaker
                    .mapNotNull { sak -> sak.digisosSoker?.metadata?.let { sak.fiksDigisosId to it } }
                    .toMap()

            val ids = soknader.keys.asFlow()

            val utbetalinger =
                fiksService
                    .getAllInnsynsfiler(
                        soknader,
                    ).parseAll()

            ids.zip(utbetalinger) { a, b -> Soknad(a, b) }
        } else {
            coroutineScope {
                digisosSaker
                    .map {
                        async { eventService.hentAlleUtbetalinger(it) }
                    }.awaitAll()
            }.associateBy { it.fiksDigisosId ?: "".also { log.warn("Manglende fiksDigisosId på model") } }
                .mapValues { (_, digisosSoker) ->
                    digisosSoker.utbetalinger
                        .filter { it.status != UtbetalingsStatus.ANNULLERT }
                        .filter {
                            it.utbetalingsDato != null || it.forfallsDato != null
                        }
                }.map { Soknad(it.key, it.value) }
                .asFlow()
        }
    }

    @WithSpan
    private fun Flow<IndexedValue<JsonDigisosSoker>>.parseAll() = map { (_, value) -> value.parseDigisosSoker() }

    private fun JsonDigisosSoker.parseDigisosSoker(): List<Utbetaling> {
        val model = InternalDigisosSoker()
        hendelser
            .filterIsInstance<JsonUtbetaling>()
            .sortedBy { it.hendelsestidspunkt }
            .forEach { model.apply(it) }
        return model.utbetalinger
            .filter { it.status != UtbetalingsStatus.ANNULLERT }
            .filter {
                it.utbetalingsDato != null || it.forfallsDato != null
            }
    }
}
