package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList


private val log = LoggerFactory.getLogger(UtbetalingerService::class.java)

@Component
class UtbetalingerService(private val eventService: EventService) {

    fun hentUtbetalinger(fiksDigisosId: String, token: String): UtbetalingerResponse {
        val model = eventService.createModel(fiksDigisosId, token)

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
                                utbetaling.vilkar.map { vilkar -> VilkarResponse(vilkar.beskrivelse, vilkar.oppfyllt) } as MutableList<VilkarResponse>,
                                utbetaling.vilkar.map { dokumentasjonKrav -> DokumentasjonkravResponse(dokumentasjonKrav.beskrivelse, dokumentasjonKrav.oppfyllt) } as MutableList<DokumentasjonkravResponse>)
                    }
            utbetalingerResponse.utbetalinger.add(UtbetalingerManedResponse(utbetalinger.key, alleUtbetalingene.toMutableList(), alleUtbetalingene.stream().map { t -> t.belop }.reduce { t, u -> t.plus(u) }.get()))
        }

        return utbetalingerResponse
    }

    private fun monthToString(localDate: LocalDate?) =
            DateFormatSymbols().months[localDate!!.get(ChronoField.MONTH_OF_YEAR) - 1]

}