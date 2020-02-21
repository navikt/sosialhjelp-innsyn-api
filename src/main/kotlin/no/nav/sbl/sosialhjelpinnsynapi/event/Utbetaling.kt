package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.common.UTBETALINGSOVERSIKT
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDate
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime
import java.math.BigDecimal

fun InternalDigisosSoker.apply(hendelse: JsonUtbetaling) {

    val utbetaling = Utbetaling(
            referanse = hendelse.utbetalingsreferanse,
            status = UtbetalingsStatus.valueOf(hendelse.status?.value() ?: JsonUtbetaling.Status.PLANLAGT_UTBETALING.value()),
            belop = BigDecimal.valueOf(hendelse.belop ?: 0.0),
            beskrivelse = hendelse.beskrivelse,
            forfallsDato = if (hendelse.forfallsdato == null) null else hendelse.forfallsdato.toLocalDate(),
            utbetalingsDato = if (hendelse.utbetalingsdato == null) null else hendelse.utbetalingsdato.toLocalDate(),
            fom = if (hendelse.fom == null) null else hendelse.fom.toLocalDate(),
            tom = if (hendelse.tom == null) null else hendelse.tom.toLocalDate(),
            mottaker = hendelse.mottaker,
            kontonummer = if (erForEnAnnenMotaker(hendelse)) null else hendelse.kontonummer,
            utbetalingsmetode = hendelse.utbetalingsmetode,
            vilkar = mutableListOf(),
            dokumentasjonkrav = mutableListOf()
    )

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse }
            ?: saker.firstOrNull { it.referanse == "default" }

    sakForReferanse?.utbetalinger?.removeIf { t -> t.referanse == utbetaling.referanse }
    sakForReferanse?.utbetalinger?.add(utbetaling)
    utbetalinger.removeIf { t -> t.referanse == utbetaling.referanse }
    utbetalinger.add(utbetaling)

    if(utbetaling.status == UtbetalingsStatus.UTBETALT) {
        val beskrivelse = if (utbetaling.utbetalingsmetode.equals(other = "bankoverføring", ignoreCase = true)) {
            if (erForEnAnnenMotaker(hendelse)) {
                "Utbetalingen for ${utbetaling.beskrivelse} har blitt sendt fra NAV til ${utbetaling.mottaker}."
            } else {
                "Utbetalingen for ${utbetaling.beskrivelse} har blitt sendt fra NAV til din konto. Du mottar pengene så fort banken har har behandlet utbetalingen."
            }
        } else {
            "Utbetalingen for ${utbetaling.beskrivelse} har blitt sendt fra NAV som ${utbetaling.utbetalingsmetode}."
        }
        historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), UrlResponse(UTBETALINGSOVERSIKT, "url til utbetalingsoversikt??"))) //FIXME url til utbetalingsoversikt
    }
}

private fun erForEnAnnenMotaker(hendelse: JsonUtbetaling) =
        hendelse.annenMottaker == null || hendelse.annenMottaker