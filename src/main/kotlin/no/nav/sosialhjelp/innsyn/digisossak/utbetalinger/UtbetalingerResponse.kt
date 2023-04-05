package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class UtbetalingerResponse(
    val ar: Int,
    val maned: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val foersteIManeden: LocalDate,
    val utbetalinger: List<ManedUtbetaling>
)
data class KommendeOgUtbetalteUtbetalingerResponse(
    val utbetalinger: List<ManedUtbetaling>,
    val ar: Int,
    val maned: String
)

data class ManedUtbetaling(
    val tittel: String,
    val belop: Double,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val utbetalingsdato: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val forfallsdato: LocalDate?,
    val status: String,
    val fiksDigisosId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate?,
    val mottaker: String?,
    val annenMottaker: Boolean,
    val kontonummer: String?,
    val utbetalingsmetode: String?,
)
