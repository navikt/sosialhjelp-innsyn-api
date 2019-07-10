package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerManedResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.format.TextStyle
import java.util.*
import java.util.stream.Collectors

private val log = LoggerFactory.getLogger(UtbetalingerService::class.java)

@Component
class UtbetalingerService(private val eventService: EventService) {

    fun hentUtbetalingerResponse(fiksDigisosId: String, token: String): UtbetalingerResponse {
        val model = eventService.createModel(fiksDigisosId)

        if (model.saker.isEmpty()) {
            log.info("Fant ingen saker for $fiksDigisosId")
            return UtbetalingerResponse(mutableListOf())
        }

        val utbetalingerResponse = UtbetalingerResponse(mutableListOf())

        val utbetalingerPerManed = model.saker.stream().flatMap { sak -> sak.utbetalinger.stream() }.collect(Collectors.groupingBy { t -> t.fom.month })

        for (mutableEntry in utbetalingerPerManed) {
            val key = mutableEntry.key
            val tittel = key.getDisplayName(TextStyle.FULL, Locale.ENGLISH)


            val utbetalinger = mutableEntry.value.map { utbetaling -> UtbetalingResponse("", utbetaling.belop.toDouble(), utbetaling.utbetalingsDato) }
           utbetalingerResponse.maned.add( UtbetalingerManedResponse(tittel, utbetalinger.toMutableList(), mutableEntry.value.stream().map { t -> t.belop}.reduce { t, u ->  t.plus(u)}.get().toDouble()))
        }

        return  utbetalingerResponse
    }

}