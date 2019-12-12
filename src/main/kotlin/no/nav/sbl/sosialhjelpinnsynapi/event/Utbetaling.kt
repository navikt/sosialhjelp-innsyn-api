package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Utbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtbetalingsStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun InternalDigisosSoker.apply(hendelse: JsonUtbetaling) {
    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val utbetaling = Utbetaling(hendelse.utbetalingsreferanse,
            UtbetalingsStatus.valueOf(hendelse.status?.value() ?: JsonUtbetaling.Status.PLANLAGT_UTBETALING.value()),
            BigDecimal.valueOf(hendelse.belop ?: 0.0),
            hendelse.beskrivelse,
            if (hendelse.forfallsdato == null) null else LocalDate.parse(hendelse.forfallsdato, pattern),
            if (hendelse.utbetalingsdato == null) null else LocalDate.parse(hendelse.utbetalingsdato, pattern),
            if (hendelse.fom == null) null else LocalDate.parse(hendelse.fom, pattern),
            if (hendelse.tom == null) null else LocalDate.parse(hendelse.tom, pattern),
            hendelse.mottaker,
            hendelse.utbetalingsmetode,
            mutableListOf(),
            mutableListOf()
    )


    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse }
            ?: saker.firstOrNull { it.referanse == "default" }

    sakForReferanse?.utbetalinger?.removeIf { t -> t.referanse == utbetaling.referanse }
    sakForReferanse?.utbetalinger?.add(utbetaling)
    utbetalinger.removeIf { t -> t.referanse == utbetaling.referanse }
    utbetalinger.add(utbetaling)
}