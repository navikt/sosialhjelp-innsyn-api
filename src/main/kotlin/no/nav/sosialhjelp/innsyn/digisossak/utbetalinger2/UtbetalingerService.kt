package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class SoknadRef(
    val fiksDigisosId: String,
    val soknadTittel: String,
    val datoSendt: LocalDateTime?,
)

data class UtbetalingMedSoknader(
    val utbetaling: Utbetaling,
    val tilknyttedeSoknader: List<SoknadRef>,
)

private data class SoknadMedUtbetalinger(
    val soknadRef: SoknadRef,
    val utbetalinger: List<Utbetaling>,
)

@Service("UtbetalingService2")
class UtbetalingerService(
    private val eventService: EventService,
    private val fiksService: FiksService,
) {
    private val log by logger()

    suspend fun hentUtbetalinger(): List<UtbetalingMedSoknader> {
        val digisosSaker = fiksService.getAllSoknader()
        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val soknader =
            coroutineScope {
                digisosSaker
                    .map { digisosSak -> async { digisosSak to eventService.hentAlleUtbetalinger(digisosSak) } }
                    .awaitAll()
            }.map { (digisosSak, model) ->
                val fiksDigisosId = model.fiksDigisosId ?: "".also { log.warn("Manglende fiksDigisosId på model") }
                val utbetalinger =
                    model.utbetalinger
                        .filter { it.status != UtbetalingsStatus.ANNULLERT }
                        .filter { it.utbetalingsDato != null || it.forfallsDato != null }
                val datoSendt =
                    digisosSak.originalSoknadNAV
                        ?.timestampSendt
                        ?.takeIf { it != 0L }
                        ?.let { unixToLocalDateTime(it) }
                SoknadMedUtbetalinger(SoknadRef(fiksDigisosId, model.getNavn(), datoSendt), utbetalinger)
            }

        val soknaderPerReferanse: Map<String, List<SoknadRef>> =
            soknader
                .flatMap { soknad -> soknad.utbetalinger.map { it.referanse to soknad.soknadRef } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, list) -> list.distinctBy { it.fiksDigisosId } }

        return soknader
            .flatMap { soknad ->
                soknad.utbetalinger.map { utbetaling ->
                    UtbetalingMedSoknader(
                        utbetaling = utbetaling,
                        tilknyttedeSoknader =
                            soknaderPerReferanse[utbetaling.referanse] ?: listOf(soknad.soknadRef),
                    )
                }
            }.distinctBy { it.utbetaling.referanse }
    }
}
