package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.utils.toLocalDate
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import java.math.BigDecimal

fun InternalDigisosSoker.apply(hendelse: JsonUtbetaling) {
    val gammelUtbetaling = utbetalinger.firstOrNull { it.referanse == hendelse.utbetalingsreferanse }
    val utbetaling =
        Utbetaling(
            referanse = hendelse.utbetalingsreferanse,
            status =
                UtbetalingsStatus.valueOf(
                    hendelse.status?.name
                        ?: JsonUtbetaling.Status.PLANLAGT_UTBETALING.name,
                ),
            belop = BigDecimal.valueOf(hendelse.belop ?: 0.0),
            beskrivelse = hendelse.beskrivelse,
            forfallsDato = hendelse.forfallsdato?.toLocalDate(),
            utbetalingsDato = hendelse.utbetalingsdato?.toLocalDate(),
            stoppetDato =
                if (hendelse.status == JsonUtbetaling.Status.STOPPET) {
                    hendelse.hendelsestidspunkt.toLocalDateTime().toLocalDate()
                } else {
                    gammelUtbetaling?.stoppetDato
                },
            fom = hendelse.fom?.toLocalDate(),
            tom = hendelse.tom?.toLocalDate(),
            mottaker = hendelse.mottaker,
            annenMottaker = isAnnenMottaker(hendelse),
            kontonummer = hendelse.kontonummer.takeUnless { isAnnenMottaker(hendelse) },
            utbetalingsmetode = hendelse.utbetalingsmetode,
            vilkar = mutableListOf(),
            dokumentasjonkrav = mutableListOf(),
            datoHendelse = hendelse.hendelsestidspunkt.toLocalDateTime(),
        )

    val sakForReferanse =
        saker.firstOrNull { it.referanse == hendelse.saksreferanse }
            ?: saker.firstOrNull { it.referanse == "default" }

    sakForReferanse?.utbetalinger?.removeIf { t -> t.referanse == utbetaling.referanse }
    sakForReferanse?.utbetalinger?.add(utbetaling)
    utbetalinger.removeIf { t -> t.referanse == utbetaling.referanse }
    utbetalinger.add(utbetaling)

    // Én historikk-hendelse per statusendring, slik at tidligere endringer (og annullering) ikke overskrives.
    // Endringer uten ny status (f.eks. nytt beløp eller ny dato) gir ingen ny hendelse.
    if (gammelUtbetaling == null || gammelUtbetaling.status != utbetaling.status) {
        historikk.add(
            Hendelse(
                hendelseType = HendelseTekstType.UTBETALINGER_OPPDATERT,
                tidspunkt = utbetaling.datoHendelse,
                utbetalingsStatus = utbetaling.status,
            ),
        )
    }
}

private fun isAnnenMottaker(hendelse: JsonUtbetaling) = hendelse.annenMottaker == true
