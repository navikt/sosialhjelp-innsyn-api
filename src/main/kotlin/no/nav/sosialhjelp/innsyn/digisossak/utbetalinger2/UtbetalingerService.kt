package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.apply
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Service

@Service("UtbetalingService2")
class UtbetalingerService(
    private val fiksService: FiksService,
) {
    private val log by logger()

    suspend fun hentUtbetalinger(): Map<String, List<Utbetaling>> {
        val digisosSaker = fiksService.getAllSoknader()
        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyMap()
        }
        val soknader =
            digisosSaker
                .mapNotNull { sak -> sak.digisosSoker?.metadata?.let { sak.fiksDigisosId to it } }
                .toMap()
        val utbetalinger =
            fiksService
                .getAllInnsynsfiler(
                    soknader,
                ).map { digisosSoker ->
                    val model = InternalDigisosSoker()
                    digisosSoker.hendelser
                        .filterIsInstance<JsonUtbetaling>()
                        .sortedBy { it.hendelsestidspunkt }
                        .forEach { model.apply(it) }
                    model.utbetalinger
                        .filter { it.status != UtbetalingsStatus.ANNULLERT }
                        .filter {
                            it.utbetalingsDato != null || it.forfallsDato != null
                        }
                }

        return soknader.keys.zip(utbetalinger).toMap()
    }
}
