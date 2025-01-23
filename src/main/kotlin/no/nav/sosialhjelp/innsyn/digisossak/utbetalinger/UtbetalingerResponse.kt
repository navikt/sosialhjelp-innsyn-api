package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingerResponse(
    val ar: Int,
    val maned: Int,
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(deprecated = true, description = "Bruk ar og maned")
    val foersteIManeden: LocalDate,
    val utbetalinger: List<ManedUtbetaling>,
)

data class NyeOgTidligereUtbetalingerResponse(
    val utbetalingerForManed: List<ManedUtbetaling>,
    val ar: Int,
    val maned: Int,
)

data class ManedUtbetaling(
    val tittel: String,
    val belop: BigDecimal,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val utbetalingsdato: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val forfallsdato: LocalDate?,
    val status: UtbetalingsStatus,
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
