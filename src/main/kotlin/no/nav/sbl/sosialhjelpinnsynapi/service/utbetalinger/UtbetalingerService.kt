package no.nav.sbl.sosialhjelpinnsynapi.service.utbetalinger

import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.ManedUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingerResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingsStatus
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.utils.coroutines.RequestContextService
import no.nav.sbl.sosialhjelpinnsynapi.utils.flatMapParallel
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.util.*


const val UTBETALING_DEFAULT_TITTEL = "Utbetaling"

@Component
class UtbetalingerService(
        private val eventService: EventService,
        private val fiksClient: FiksClient,
        private val requestContextService: RequestContextService
) {

    fun hentUtbetalinger(token: String, months: Int): List<UtbetalingerResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

//        val alleUtbetalinger: List<ManedUtbetaling> =
//                    digisosSaker
//                            .filter { isDigisosSakNewerThanMonths(it, months) }
//                            .flatMap { manedsutbetalinger(token, it) }

        val start = System.currentTimeMillis()
        val alleUtbetalinger = runBlocking(requestContextService.getCoroutineContext()) {
            digisosSaker
                    .filter { isDigisosSakNewerThanMonths(it, months) }
                    .flatMapParallel { manedsutbetalinger(token, it) }
        }
        log.info("hentAlleUtbetalinger (før gruppering): ${System.currentTimeMillis()-start}ms")

        val start2 = System.currentTimeMillis()
        return alleUtbetalinger
                .sortedByDescending { it.utbetalingsdato }
                .groupBy { YearMonth.of(it.utbetalingsdato!!.year, it.utbetalingsdato.month) }
                .map { (key, value) ->
                    UtbetalingerResponse(
                            ar = key.year,
                            maned = monthToString(key.monthValue),
                            foersteIManeden = foersteIManeden(key),
                            utbetalinger = value.sortedByDescending { it.utbetalingsdato }
                    )
                }.also { log.info("hentAlleUtbetalinger (gruppering): ${System.currentTimeMillis()-start2}ms") }
    }

    private fun manedsutbetalinger(token: String, digisosSak: DigisosSak): List<ManedUtbetaling> {
        val model = eventService.hentAlleUtbetalinger(token, digisosSak)
        return model.utbetalinger
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
                            annenMottaker = utbetaling.annenMottaker,
                            kontonummer = utbetaling.kontonummer,
                            utbetalingsmetode = utbetaling.utbetalingsmetode
                    )
                }
    }

    fun isDigisosSakNewerThanMonths(digisosSak: DigisosSak, months: Int): Boolean {
        return digisosSak.sistEndret >= DateTime.now().minusMonths(months).millis
    }

    fun isDateNewerThanMonths(date: LocalDate, months: Int): Boolean {
        return date >= LocalDate.now().minusMonths(months.toLong())
    }

    fun containsUtbetalingNewerThanMonth(model: InternalDigisosSoker, months: Int): Boolean {
        return model.utbetalinger
                .any {
                    it.status == UtbetalingsStatus.UTBETALT
                            && it.utbetalingsDato != null
                            && isDateNewerThanMonths(it.utbetalingsDato!!, months)
                }
    }

    fun utbetalingExists(token: String, months: Int): Boolean {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return false
        }

        return digisosSaker
                .filter { digisosSak -> isDigisosSakNewerThanMonths(digisosSak, months) }
                .any { digisosSak ->
                    val model = eventService.hentAlleUtbetalinger(token, digisosSak)
                    (containsUtbetalingNewerThanMonth(model, months))
                }
    }

    private fun foersteIManeden(key: YearMonth) = LocalDate.of(key.year, key.month, 1)

    private fun monthToString(month: Int) = DateFormatSymbols(Locale.forLanguageTag("no-NO")).months[month - 1]

    companion object {
        private val log by logger()
    }
}
