package no.nav.sosialhjelp.innsyn.digisossak.utbetalinger2

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.sosialhjelp.innsyn.digisossak.utbetalinger.ManedUtbetaling
import no.nav.sosialhjelp.innsyn.digisossak.utbetalinger.UtbetalingerService.Companion.UTBETALING_DEFAULT_TITTEL
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingDto(
    val referanse: String,
    val tittel: String,
    val belop: BigDecimal,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val utbetalingsdato: LocalDate?,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val forfallsdato: LocalDate?,
    val status: UtbetalingsStatus,
    val fiksDigisosId: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val fom: LocalDate?,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val tom: LocalDate?,
    val mottaker: String?,
    val annenMottaker: Boolean,
    @param:Schema(pattern = "^[0-9]{11}$")
    val kontonummer: String?,
    val utbetalingsmetode: String?,
)

fun Utbetaling.toDto(fiksDigisosId: String) =
    UtbetalingDto(
        referanse = this.referanse,
        tittel = this.beskrivelse ?: UTBETALING_DEFAULT_TITTEL,
        belop = this.belop,
        utbetalingsdato = this.utbetalingsDato,
        forfallsdato = this.forfallsDato,
        status = this.status,
        fiksDigisosId = fiksDigisosId,
        fom = this.fom,
        tom = this.tom,
        mottaker = this.mottaker,
        annenMottaker = this.annenMottaker,
        kontonummer = this.kontonummer,
        utbetalingsmetode = this.utbetalingsmetode,
    )
