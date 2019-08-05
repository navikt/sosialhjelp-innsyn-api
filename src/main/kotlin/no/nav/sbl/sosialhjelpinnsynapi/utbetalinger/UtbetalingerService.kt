package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import no.nav.sbl.sosialhjelpinnsynapi.domain.Utbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerManedResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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

        val utbetalingerPerManed = HashMap<Month, MutableList<Utbetaling>>().withDefault { key -> ArrayList() }
        model.saker.stream().flatMap { t -> t.utbetalinger.stream() }.collect(Collectors.toList()).forEach { utbetaling ->
            utbetalingerPerManed[utbetaling.fom.month]!!.add(utbetaling)
        }
        for (utbetalinger in utbetalingerPerManed.entries) {
            val key = utbetalinger.key
            val tittel = key.getDisplayName(TextStyle.FULL, Locale.ENGLISH)


            val utbetalinger = utbetalinger.value.map { utbetaling -> UtbetalingResponse("", utbetaling.belop.toDouble(), utbetaling.utbetalingsDato) }
            utbetalingerResponse.maned.add(UtbetalingerManedResponse(tittel, utbetalinger.toMutableList(), utbetalinger.stream().map { t -> t.belop }.reduce { t, u -> t.plus(u) }.get()))
        }

        return utbetalingerResponse
    }

}