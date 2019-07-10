package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun InternalDigisosSoker.applyForelopigSvar(hendelse: JsonUtbetaling, clientProperties: ClientProperties) {
    hendelse.belop
    val pattern = DateTimeFormatter.ofPattern("YYYY-MM-DD")
    Utbetaling(hendelse.utbetalingsreferanse,
            UtbetalingsStatus.valueOf(hendelse.status.value()),
            BigDecimal.valueOf(hendelse.belop),
            "",//  hendelse.beskrivelse,
            LocalDate.parse(hendelse.posteringsdato, pattern),
            LocalDate.parse(hendelse.utbetalingsdato, pattern),
            LocalDate.parse(hendelse.fom, pattern),
            LocalDate.parse(hendelse.tom, pattern),
            hendelse.mottaker,
            "utbetalingsform"
    )
    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse }

    if (sakForReferanse != null) {
        sakForReferanse.utbetalinger.firstOrNull { it.referanse == hendelse.utbetalingsreferanse }
    } else {
        // Opprett ny Sak
        saker.add(Sak(
                hendelse.saksreferanse,
                SaksStatus.UNDER_BEHANDLING,
                "Sak om sosialhjelp",
                mutableListOf(),
                mutableListOf()
        ))
    }

}