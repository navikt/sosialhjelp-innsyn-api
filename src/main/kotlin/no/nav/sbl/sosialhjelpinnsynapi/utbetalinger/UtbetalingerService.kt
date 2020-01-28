package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.YearMonth


const val UTBETALING_DEFAULT_TITTEL = "Utbetaling"

@Component
class UtbetalingerService(private val eventService: EventService,
                          private val fiksClient: FiksClient) {

    companion object {
        val log by logger()
    }

    fun hentUtbetalinger(token: String, months: Int): List<UtbetalingerResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val alleUtbetalinger: List<ManedUtbetaling> = digisosSaker
                .filter { digisosSak -> digisosSak.sistEndret >= DateTime.now().minusMonths(months).millis }
                .flatMap { digisosSak ->
                    val model = eventService.hentAlleUtbetalinger(token, digisosSak)
                        model.utbetalinger
                                .filter { it.utbetalingsDato != null && it.status == UtbetalingsStatus.UTBETALT }
                                .map { utbetaling ->
                                    ManedUtbetaling(
                                            tittel = utbetaling.beskrivelse ?: UTBETALING_DEFAULT_TITTEL,
                                            belop = utbetaling.belop.toDouble(),
                                            utbetalingsdato = utbetaling.utbetalingsDato,
                                            forfallsdato = utbetaling.forfallsDato,
                                            status = utbetaling.status.name,
                                            fiksDigisosId = digisosSak.fiksDigisosId,
                                            fom = utbetaling.fom,
                                            tom = utbetaling.tom,
                                            mottaker = utbetaling.mottaker,
                                            kontonummer = utbetaling.kontonummer,
                                            utbetalingsmetode = utbetaling.utbetalingsmetode
                                    )
                                }
                }

        return alleUtbetalinger
                .sortedByDescending { it.utbetalingsdato}
                .groupBy { YearMonth.of(it.utbetalingsdato!!.year, it.utbetalingsdato.month) }
                .map { (key, value) ->
                    UtbetalingerResponse(
                            ar = key.year,
                            maned = monthToString(key.monthValue),
                            foersteIManeden = foersteIManeden(key),
                            utbetalinger = value.sortedByDescending { it.utbetalingsdato }
                    )
                }
    }

    private fun foersteIManeden(key: YearMonth) = LocalDate.of(key.year, key.month, 1)

    private fun monthToString(month: Int) = DateFormatSymbols().months[month - 1]

}