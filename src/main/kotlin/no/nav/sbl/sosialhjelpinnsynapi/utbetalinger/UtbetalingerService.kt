package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import no.nav.sbl.sosialhjelpinnsynapi.domain.ManedUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingsStatus
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.logger
import org.springframework.stereotype.Component
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.YearMonth


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

        val alleUtbetalinger: List<ManedUtbetaling> = digisosSaker
                .flatMap { digisosSak ->
                    val model = eventService.createModel(digisosSak, token)
                    model.saker
                            .flatMap { sak ->
                                sak.utbetalinger
                                        .filter { it.utbetalingsDato != null && (it.status == UtbetalingsStatus.UTBETALT || it.status == UtbetalingsStatus.ANNULLERT) }
                                        .map { utbetaling ->
                                            ManedUtbetaling(
                                                    tittel = utbetaling.beskrivelse,
                                                    belop = utbetaling.belop.toDouble(),
                                                    utbetalingsdato = utbetaling.utbetalingsDato,
                                                    status = utbetaling.status.name,
                                                    fiksDigisosId = digisosSak.fiksDigisosId,
                                                    fom = utbetaling.fom,
                                                    tom = utbetaling.tom,
                                                    annenMottaker = utbetaling.mottaker,
                                                    kontonummer = utbetaling.kontonummer
                                            )
                                        }
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
                            sum = value.filter { it.status == UtbetalingsStatus.UTBETALT.name }.sumByDouble { it.belop },
                            utbetalinger = value.sortedByDescending { it.utbetalingsdato }
                    )
                }
    }

    private fun foersteIManeden(key: YearMonth) = LocalDate.of(key.year, key.month, 1)

    private fun monthToString(month: Int) = DateFormatSymbols().months[month - 1]

}