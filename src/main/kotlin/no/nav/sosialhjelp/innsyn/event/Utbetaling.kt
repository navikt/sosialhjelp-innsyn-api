package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.toLocalDate
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import java.math.BigDecimal

fun InternalDigisosSoker.apply(hendelse: JsonUtbetaling) {
    val log by logger()

    if (hendelse.utbetalingsdato == null) log.debug("utbetalingsdato er null, selv om leverandorene har kommunisert at de alltid sender denne.")
    if (hendelse.fom == null) log.info("utbetalingens start-periode (fom) er null")
    if (hendelse.tom == null) log.info("utbetalingens slutt-periode (tom) er null")
    if (hendelse.status == null) log.info("utbetalingsstatus er null")
    if (hendelse.status == JsonUtbetaling.Status.PLANLAGT_UTBETALING) log.info("utbetalingsstatus er PLANLAGT_UTBETALING")


    val utbetaling = Utbetaling(
        referanse = hendelse.utbetalingsreferanse,
        status = UtbetalingsStatus.valueOf(
            hendelse.status?.value()
                ?: JsonUtbetaling.Status.PLANLAGT_UTBETALING.value()
        ),
        belop = BigDecimal.valueOf(hendelse.belop ?: 0.0),
        beskrivelse = hendelse.beskrivelse,
        forfallsDato = if (hendelse.forfallsdato == null) null else hendelse.forfallsdato.toLocalDate(),
        utbetalingsDato = if (hendelse.utbetalingsdato == null) null else hendelse.utbetalingsdato.toLocalDate(),
        fom = if (hendelse.fom == null) null else hendelse.fom.toLocalDate(),
        tom = if (hendelse.tom == null) null else hendelse.tom.toLocalDate(),
        mottaker = hendelse.mottaker,
        annenMottaker = isAnnenMottaker(hendelse),
        kontonummer = if (isAnnenMottaker(hendelse)) null else hendelse.kontonummer,
        utbetalingsmetode = hendelse.utbetalingsmetode,
        vilkar = mutableListOf(),
        dokumentasjonkrav = mutableListOf(),
        datoHendelse = hendelse.hendelsestidspunkt.toLocalDateTime()
    )

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse }
        ?: saker.firstOrNull { it.referanse == "default" }

    sakForReferanse?.utbetalinger?.removeIf { t -> t.referanse == utbetaling.referanse }
    sakForReferanse?.utbetalinger?.add(utbetaling)
    utbetalinger.removeIf { t -> t.referanse == utbetaling.referanse }
    utbetalinger.add(utbetaling)
}

private fun isAnnenMottaker(hendelse: JsonUtbetaling) =
    hendelse.annenMottaker == null || hendelse.annenMottaker
