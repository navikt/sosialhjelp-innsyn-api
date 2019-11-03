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
import kotlin.collections.ArrayList


@Component
class UtbetalingerService(private val eventService: EventService,
                          private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

    fun hentUtbetalinger(token: String): List<UtbetalingerResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val responseList: List<UtbetalingerResponse> = digisosSaker.map {
            val model = eventService.createModel(it, token)
            val utbetalingerResponse = UtbetalingerResponse(it.fiksDigisosId, mutableListOf())
            val utbetalingerPerManed = TreeMap<String, MutableList<Utbetaling>>()

            model.saker
                    .flatMap { sak -> sak.utbetalinger }
                    .forEach { utbetaling ->
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

            utbetalingerPerManed.entries
                    .forEach {
                        val alleUtbetalingene = it.value
                                .map { utbetaling ->
                                    UtbetalingResponse(
                                            utbetaling.beskrivelse,
                                            utbetaling.belop.toDouble(),
                                            utbetaling.utbetalingsDato,
                                            utbetaling.vilkar
                                                    .map { vilkar -> VilkarResponse(vilkar.beskrivelse, vilkar.oppfyllt) }
                                                    .toMutableList(),
                                            utbetaling.dokumentasjonkrav
                                                    .map { dokumentasjonskrav -> DokumentasjonskravResponse(dokumentasjonskrav.beskrivelse, dokumentasjonskrav.oppfyllt) }
                                                    .toMutableList())
                                }
                        utbetalingerResponse.utbetalinger.add(UtbetalingerManedResponse(
                                it.key,
                                alleUtbetalingene.toMutableList(),
                                alleUtbetalingene
                                        .map { t -> t.belop }
                                        .reduce { t, u -> t.plus(u) }))
                    }

            utbetalingerResponse
        }

        return responseList
    }

    private fun monthToString(localDate: LocalDate?) =
            DateFormatSymbols().months[localDate!!.get(ChronoField.MONTH_OF_YEAR) - 1]

}