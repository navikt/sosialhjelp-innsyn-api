package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.isNewerThanMonths
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.flatMapParallel
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.RequestContextHolder.setRequestAttributes
import java.time.LocalDate
import java.time.YearMonth

@Component
class UtbetalingerService(
    private val eventService: EventService,
    private val fiksClient: FiksClient,
) {
    fun hentUtbetalingerForSak(
        fiksDigisosId: String,
        token: String,
    ): List<UtbetalingerResponse> {
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
        return toUtbetalingerResponse(manedsutbetalinger(token, digisosSak) { true })
    }

    fun hentUtbetalteUtbetalinger(
        token: String,
        months: Int,
    ): List<UtbetalingerResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val requestAttributes = RequestContextHolder.getRequestAttributes()

        val alleUtbetalinger =
            runBlocking(Dispatchers.IO + MDCContext()) {
                digisosSaker
                    .filter { it.isNewerThanMonths(months) }
                    .flatMapParallel {
                        setRequestAttributes(requestAttributes)
                        manedsutbetalinger(token, it) { status -> status == UtbetalingsStatus.UTBETALT }
                    }
            }
        return toUtbetalingerResponse(alleUtbetalinger)
    }

    private fun hentUtbetalinger(
        token: String,
        statusFilter: (status: UtbetalingsStatus) -> Boolean,
    ): List<ManedUtbetaling> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val requestAttributes = RequestContextHolder.getRequestAttributes()

        return runBlocking(Dispatchers.IO + MDCContext()) {
            digisosSaker
                .filter { it.isNewerThanMonths(15) }
                .flatMapParallel {
                    setRequestAttributes(requestAttributes)
                    manedsutbetalinger(token, it, statusFilter)
                }
        }
    }

    fun hentTidligereUtbetalinger(token: String): List<NyeOgTidligereUtbetalingerResponse> {
        val utbetalinger =
            hentUtbetalinger(token) { status -> (status == UtbetalingsStatus.UTBETALT || status == UtbetalingsStatus.STOPPET) }
        return toTidligereUtbetalingerResponse(utbetalinger)
    }

    fun hentNyeUtbetalinger(token: String): List<NyeOgTidligereUtbetalingerResponse> {
        val utbetalinger = hentUtbetalinger(token) { status -> (status !== UtbetalingsStatus.ANNULLERT) }
        return toNyeUtbetalingerResponse(utbetalinger)
    }

    private fun toTidligereUtbetalingerResponse(manedUtbetalinger: List<ManedUtbetaling>): List<NyeOgTidligereUtbetalingerResponse> {
        val now = LocalDate.now()
        val yearMonth = YearMonth.of(now.year, now.month)
        val foresteIMnd = foersteIManeden(yearMonth)
        val tidligere =
            manedUtbetalinger
                .sortedByDescending { it.utbetalingsdato }
                .filter { it.utbetalingsdato?.isBefore(foresteIMnd) ?: false }
                .groupBy { YearMonth.of(it.utbetalingsdato!!.year, it.utbetalingsdato.month) }
                .map { (key, value) ->
                    NyeOgTidligereUtbetalingerResponse(
                        ar = key.year,
                        maned = key.monthValue,
                        utbetalingerForManed = value.sortedByDescending { it.utbetalingsdato },
                    )
                }

        return tidligere
    }

    private fun toNyeUtbetalingerResponse(manedUtbetalinger: List<ManedUtbetaling>): List<NyeOgTidligereUtbetalingerResponse> {
        val now = LocalDate.now()
        val yearMonth = YearMonth.of(now.year, now.month)
        val foresteIMnd = foersteIManeden(yearMonth)
        val nye =
            manedUtbetalinger
                .sortedBy { it.utbetalingsdato }
                .filter {
                    it.utbetalingsdato?.isAfter(foresteIMnd) ?: false ||
                        it.utbetalingsdato?.isEqual(foresteIMnd) ?: false ||
                        it.status == UtbetalingsStatus.PLANLAGT_UTBETALING.toString()
                }
                .groupBy { YearMonth.of(it.utbetalingsdato!!.year, it.utbetalingsdato.month) }
                .map { (key, value) ->
                    NyeOgTidligereUtbetalingerResponse(
                        ar = key.year,
                        maned = key.monthValue,
                        utbetalingerForManed = value.sortedBy { it.utbetalingsdato },
                    )
                }

        return nye
    }

    private fun toUtbetalingerResponse(manedUtbetalinger: List<ManedUtbetaling>) =
        manedUtbetalinger
            .sortedByDescending { it.utbetalingsdato }
            .groupBy { YearMonth.of(it.utbetalingsdato!!.year, it.utbetalingsdato.month) }
            .map { (key, value) ->
                UtbetalingerResponse(
                    ar = key.year,
                    maned = key.monthValue,
                    foersteIManeden = foersteIManeden(key),
                    utbetalinger = value.sortedByDescending { it.utbetalingsdato },
                )
            }

    private fun manedsutbetalinger(
        token: String,
        digisosSak: DigisosSak,
        statusFilter: (status: UtbetalingsStatus) -> Boolean,
    ): List<ManedUtbetaling> {
        val model = eventService.hentAlleUtbetalinger(token, digisosSak)
        return model.utbetalinger
            .filter { it.utbetalingsDato != null && statusFilter(it.status) }
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
                    utbetalingsmetode = utbetaling.utbetalingsmetode,
                )
            }
    }

    fun isDateNewerThanMonths(
        date: LocalDate,
        months: Int,
    ): Boolean {
        return date >= LocalDate.now().minusMonths(months.toLong())
    }

    fun containsUtbetalingNewerThanMonth(
        model: InternalDigisosSoker,
        months: Int,
    ): Boolean {
        return model.utbetalinger
            .any {
                it.status == UtbetalingsStatus.UTBETALT &&
                    it.utbetalingsDato != null &&
                    isDateNewerThanMonths(it.utbetalingsDato!!, months)
            }
    }

    fun utbetalingExists(
        token: String,
        months: Int,
    ): Boolean {
        val digisosSaker = fiksClient.hentAlleDigisosSaker(token)

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return false
        }
        return digisosSaker
            .asSequence()
            .filter { it.isNewerThanMonths(months) }
            .any {
                val model = eventService.hentAlleUtbetalinger(token, it)
                (containsUtbetalingNewerThanMonth(model, months))
            }
    }

    private fun foersteIManeden(key: YearMonth) = LocalDate.of(key.year, key.month, 1)

    private fun Utbetaling.infoLoggVedManglendeUtbetalingsDatoEllerForfallsDato(kommunenummer: String) {
        when {
            status == UtbetalingsStatus.UTBETALT && utbetalingsDato == null -> {
                log.info(
                    "Utbetaling ($referanse) med status=${UtbetalingsStatus.UTBETALT} har ikke utbetalingsDato. Kommune=$kommunenummer",
                )
            }
            status == UtbetalingsStatus.PLANLAGT_UTBETALING && forfallsDato == null -> {
                log.info(
                    "Utbetaling ($referanse) med status=${UtbetalingsStatus.PLANLAGT_UTBETALING} " +
                        "har ikke forfallsDato. Kommune=$kommunenummer",
                )
            }
            status == UtbetalingsStatus.STOPPET && (forfallsDato == null || utbetalingsDato == null) -> {
                log.info(
                    "Utbetaling ($referanse) med status=${UtbetalingsStatus.STOPPET} mangler forfallsDato eller utbetalingsDato." +
                        " Kommune=$kommunenummer",
                )
            }
        }
    }

    companion object {
        private val log by logger()

        const val UTBETALING_DEFAULT_TITTEL = "default_utbetaling_tittel"
    }
}
