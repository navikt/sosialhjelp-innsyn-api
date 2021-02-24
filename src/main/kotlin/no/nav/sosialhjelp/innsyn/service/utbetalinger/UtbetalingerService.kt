package no.nav.sosialhjelp.innsyn.service.utbetalinger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.ManedUtbetaling
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingerResponse
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.flatMapParallel
import no.nav.sosialhjelp.innsyn.utils.logger
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.RequestContextHolder.setRequestAttributes
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale


const val UTBETALING_DEFAULT_TITTEL = "Utbetaling"

@Component
class UtbetalingerService(
        private val eventService: EventService,
        private val fiksClient: FiksClient
) {

    fun hentUtbetalinger(token: String, months: Int): List<UtbetalingerResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val requestAttributes = RequestContextHolder.getRequestAttributes()

        val alleUtbetalinger = runBlocking(Dispatchers.IO + MDCContext()) {
            digisosSaker
                    .filter { isDigisosSakNewerThanMonths(it, months) }
                    .flatMapParallel {
                        setRequestAttributes(requestAttributes)
                        manedsutbetalinger(token, it)
                    }
        }

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
                }
    }

    private suspend fun manedsutbetalinger(token: String, digisosSak: DigisosSak): List<ManedUtbetaling> {
        val model = eventService.hentAlleUtbetalinger(token, digisosSak)
        return model.utbetalinger
                .filter { it.utbetalingsDato != null && it.status == UtbetalingsStatus.UTBETALT }
                .map { utbetaling ->
                    utbetaling.infoLoggVedManglendeUtbetalingsDatoEllerForfallsDato(digisosSak.kommunenummer)
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
                .asSequence()
                .filter { digisosSak -> isDigisosSakNewerThanMonths(digisosSak, months) }
                .any { digisosSak ->
                    val model = eventService.hentAlleUtbetalinger(token, digisosSak)
                    (containsUtbetalingNewerThanMonth(model, months))
                }
    }

    private fun foersteIManeden(key: YearMonth) = LocalDate.of(key.year, key.month, 1)

    private fun monthToString(month: Int) = DateFormatSymbols(Locale.forLanguageTag("no-NO")).months[month - 1]

    private fun Utbetaling.infoLoggVedManglendeUtbetalingsDatoEllerForfallsDato(kommunenummer: String) {
        when {
            status == UtbetalingsStatus.UTBETALT && utbetalingsDato == null -> {
                log.info("Utbetaling ($referanse) med status=${UtbetalingsStatus.UTBETALT} har ikke utbetalingsDato. Kommune=$kommunenummer")
            }
            status == UtbetalingsStatus.PLANLAGT_UTBETALING && forfallsDato == null -> {
                log.info("Utbetaling ($referanse) med status=${UtbetalingsStatus.PLANLAGT_UTBETALING} har ikke forfallsDato. Kommune=$kommunenummer")
            }
            status == UtbetalingsStatus.STOPPET && (forfallsDato == null || utbetalingsDato == null) -> {
                log.info("Utbetaling ($referanse) med status=${UtbetalingsStatus.STOPPET} mangler forfallsDato eller utbetalingsDato. Kommune=$kommunenummer")
            }
        }
    }

    companion object {
        private val log by logger()
    }
}
