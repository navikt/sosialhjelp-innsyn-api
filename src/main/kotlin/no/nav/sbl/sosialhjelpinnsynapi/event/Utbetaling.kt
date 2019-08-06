package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun InternalDigisosSoker.apply(hendelse: JsonUtbetaling, clientProperties: ClientProperties) {
    hendelse.belop
    val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val utbetaling = Utbetaling(hendelse.utbetalingsreferanse,
            UtbetalingsStatus.valueOf(hendelse.status.value()),
            BigDecimal.valueOf(hendelse.belop),
            hendelse.beskrivelse,
            if (hendelse.posteringsdato == null) null else LocalDate.parse(hendelse.posteringsdato, pattern),
            if (hendelse.utbetalingsdato == null) null else LocalDate.parse(hendelse.utbetalingsdato, pattern),
            if (hendelse.fom == null) null else LocalDate.parse(hendelse.fom, pattern),
            if (hendelse.tom == null) null else LocalDate.parse(hendelse.tom, pattern),
            hendelse.mottaker,
            "utbetalingsform"
    )
    var sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse }

    if (sakForReferanse != null) {
        sakForReferanse.utbetalinger.firstOrNull { it.referanse == hendelse.utbetalingsreferanse }
    } else {
        // Opprett ny Sak
        sakForReferanse = Sak(
                hendelse.saksreferanse,
                SaksStatus.UNDER_BEHANDLING,
                "Sak om sosialhjelp",
                mutableListOf(),
                mutableListOf()
        )
        saker.add(sakForReferanse)
    }
    sakForReferanse.utbetalinger.add(utbetaling)

}