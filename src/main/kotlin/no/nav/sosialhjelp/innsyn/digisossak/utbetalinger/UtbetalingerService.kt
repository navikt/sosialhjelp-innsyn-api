package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.isNewerThanMonths
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

@Component
class UtbetalingerService(
    private val eventService: EventService,
    private val fiksClient: FiksClient,
) {
    suspend fun hentUtbetalteUtbetalinger(months: Int): List<UtbetalingerResponse> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker()

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        val alleUtbetalinger =
            digisosSaker
                .filter { it.isNewerThanMonths(months) }
                .flatMap {
                    manedsutbetalinger(
                        it,
                    ) { status -> status == UtbetalingsStatus.UTBETALT || status == UtbetalingsStatus.PLANLAGT_UTBETALING }
                }
        return toUtbetalingerResponse(alleUtbetalinger)
    }

    private suspend fun hentUtbetalinger(statusFilter: (status: UtbetalingsStatus) -> Boolean): List<ManedUtbetaling> {
        val digisosSaker = fiksClient.hentAlleDigisosSaker()

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return emptyList()
        }

        return digisosSaker
            .filter { it.isNewerThanMonths(15) }
            .flatMap {
                manedsutbetalinger(it, statusFilter)
            }
    }

    suspend fun hentTidligereUtbetalinger(): List<NyeOgTidligereUtbetalingerResponse> {
        val utbetalinger =
            hentUtbetalinger { status -> (status == UtbetalingsStatus.UTBETALT || status == UtbetalingsStatus.STOPPET) }
        return toTidligereUtbetalingerResponse(utbetalinger)
    }

    suspend fun hentNyeUtbetalinger(): List<NyeOgTidligereUtbetalingerResponse> {
        val utbetalinger = hentUtbetalinger { status -> (status !== UtbetalingsStatus.ANNULLERT) }
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
                .asSequence()
                .sortedBy { it.utbetalingsdato ?: it.forfallsdato }
                .filter {
                    it.utbetalingsdato?.isAfter(foresteIMnd) ?: false ||
                        it.utbetalingsdato?.isEqual(foresteIMnd) ?: false ||
                        it.status == UtbetalingsStatus.PLANLAGT_UTBETALING
                }.filter {
                    it.utbetalingsdato != null || it.forfallsdato != null
                }.groupBy {
                    val year =
                        it.utbetalingsdato?.year
                            ?: it.forfallsdato?.year
                            ?: error("Fant ikke en dato å gruppere utbetaling på (utbetalingsdato og forfallsdato er null)")
                    val month =
                        it.utbetalingsdato?.month
                            ?: it.forfallsdato?.month
                            ?: error("Fant ikke en dato å gruppere utbetaling på (utbetalingsdato og forfallsdato er null)")
                    YearMonth.of(year, month)
                }.map { (key, value) ->
                    NyeOgTidligereUtbetalingerResponse(
                        ar = key.year,
                        maned = key.monthValue,
                        utbetalingerForManed = value.sortedBy { it.utbetalingsdato ?: it.forfallsdato },
                    )
                }.toList()

        return nye
    }

    private fun toUtbetalingerResponse(manedUtbetalinger: List<ManedUtbetaling>) =
        manedUtbetalinger
            .sortedByDescending { it.utbetalingsdato ?: it.forfallsdato }
            .groupBy { utbetaling ->
                utbetaling.utbetalingsdato?.let { YearMonth.of(it.year, it.month) }
                    ?: utbetaling.forfallsdato?.let { YearMonth.of(it.year, it.month) }
                    ?: YearMonth.of(Year.MIN_VALUE, 1)
            }.map { (key, value) ->
                UtbetalingerResponse(
                    ar = key.year,
                    maned = key.monthValue,
                    foersteIManeden = foersteIManeden(key),
                    utbetalinger = value.sortedByDescending { it.utbetalingsdato ?: it.forfallsdato },
                )
            }

    private suspend fun manedsutbetalinger(
        digisosSak: DigisosSak,
        statusFilter: (status: UtbetalingsStatus) -> Boolean,
    ): List<ManedUtbetaling> {
        val model = eventService.hentAlleUtbetalinger(digisosSak)
        return model.utbetalinger
            .filter { statusFilter(it.status) }
            .map { utbetaling ->
                utbetaling.infoLoggVedManglendeUtbetalingsDatoEllerForfallsDato(digisosSak.kommunenummer)
                ManedUtbetaling(
                    tittel = utbetaling.beskrivelse ?: UTBETALING_DEFAULT_TITTEL,
                    belop = utbetaling.belop,
                    utbetalingsdato = utbetaling.utbetalingsDato,
                    forfallsdato = utbetaling.forfallsDato,
                    status = utbetaling.status,
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
    ): Boolean = date >= LocalDate.now().minusMonths(months.toLong())

    fun containsUtbetalingNewerThanMonth(
        model: InternalDigisosSoker,
        months: Int,
    ): Boolean =
        model.utbetalinger
            .any {
                it.status == UtbetalingsStatus.UTBETALT &&
                    it.utbetalingsDato != null &&
                    isDateNewerThanMonths(it.utbetalingsDato!!, months)
            }

    suspend fun utbetalingExists(
        token: Token,
        months: Int,
    ): Boolean {
        val digisosSaker = fiksClient.hentAlleDigisosSaker()

        if (digisosSaker.isEmpty()) {
            log.info("Fant ingen søknader for bruker")
            return false
        }
        return digisosSaker
            .asSequence()
            .filter { it.isNewerThanMonths(months) }
            .any {
                val model = eventService.hentAlleUtbetalinger(it)
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
