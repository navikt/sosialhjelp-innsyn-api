package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Service

@Service("UtbetalingService2")
class UtbetalingerService(
    private val eventService: EventService,
    private val fiksService: FiksService,
) {
    private val log by logger()

    suspend fun hentUtbetalinger(): Map<String, List<Utbetaling>> {
        val digisosSaker = fiksService.getAllSoknader()
        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyMap()
        }

        val utbetalinger =
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
                }

        return utbetalinger
    }
}
