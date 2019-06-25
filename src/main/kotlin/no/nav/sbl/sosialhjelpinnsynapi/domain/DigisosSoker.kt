package no.nav.sbl.sosialhjelpinnsynapi.domain

import java.math.BigDecimal
import java.time.LocalDate

data class Soknad(
        val referanse: String,
        val status: String,
        val saker: Collection<Sak>,
        val forvaltninsbrev: Collection<Forvaltningsbrev>
)

data class Forvaltningsbrev(
        val referanse: String,
        val tittel: String
)

data class Sak(
        val referanse: String,
        val saksStatus: SaksStatus,
        val tittel: String,
        val vedtak: Collection<Vedtak>,
        val utbetalinger: Collection<Utbetaling>
)

data class Vedtak(
        val utfall: UtfallVedtak
)

data class Utbetaling(
        val referanse: String,
        val status: UtbetalingsStatus,
        val belop: BigDecimal,
        val beskrivelse: String,
        val posteringsDato: LocalDate,
        val fom: LocalDate,
        val tom: LocalDate,
        val mottaker: String,
        val utbetalingsform: String
)

data class Vilkar(
        val referanse: String,
        val utbetalinger: Collection<Utbetaling>,
        val beskrivelse: String
)

enum class SoknadsStatus {
    MOTTATT, UNDER_BEHANDLING, FERDIGBEHANDLET
}
enum class SaksStatus {
    UNDER_BEHANDLING, IKKE_INNSYN
}

enum class UtbetalingsStatus {
    PLANLAGT_UTBETALING, UTBETALT, STOPPET
}

enum class UtfallVedtak {
    INNVILGET, DELVIS_INNVILGET, AVSLATT, AVVIST
}