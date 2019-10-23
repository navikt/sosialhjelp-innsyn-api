package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList


@Component
class UtbetalingerService(private val eventService: EventService,
                          private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

    fun hentUtbetalinger(fiksDigisosId: String, token: String): UtbetalingerResponse {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        val model = eventService.createModel(digisosSak, token)

        if (model.saker.isEmpty()) {
            log.info("Fant ingen saker for $fiksDigisosId")
            return UtbetalingerResponse(mutableListOf())
        }

        val utbetalingerResponse = UtbetalingerResponse(mutableListOf())

        val utbetalingerPerManed = TreeMap<String, MutableList<Utbetaling>>()
        model.saker.stream().flatMap { t -> t.utbetalinger.stream() }.collect(Collectors.toList()).forEach { utbetaling ->
            if (utbetaling.utbetalingsDato != null) {
                val monthToString = monthToString(utbetaling.utbetalingsDato)
                if (!utbetalingerPerManed.containsKey(monthToString)) {
                    utbetalingerPerManed[monthToString] = ArrayList()
                }
                utbetalingerPerManed[monthToString]!!.add(utbetaling)

            }
            if (utbetaling.fom != null) {
                utbetalingerPerManed[monthToString(utbetaling.fom)]!!.add(utbetaling)
            }
        }
        for (utbetalinger in utbetalingerPerManed.entries) {
            val alleUtbetalingene =
                    utbetalinger.value.map { utbetaling ->
                        UtbetalingResponse(utbetaling.beskrivelse, utbetaling.belop.toDouble(), utbetaling.utbetalingsDato,
                                utbetaling.vilkar.map { vilkar -> VilkarResponse(vilkar.beskrivelse, vilkar.oppfyllt) } as MutableList<VilkarResponse>)
                    }
            utbetalingerResponse.utbetalinger.add(UtbetalingerManedResponse(utbetalinger.key, alleUtbetalingene.toMutableList(), alleUtbetalingene.stream().map { t -> t.belop }.reduce { t, u -> t.plus(u) }.get()))
        }

        return utbetalingerResponse
    }

    private fun monthToString(localDate: LocalDate?) =
            DateFormatSymbols().months[localDate!!.get(ChronoField.MONTH_OF_YEAR) - 1]

}