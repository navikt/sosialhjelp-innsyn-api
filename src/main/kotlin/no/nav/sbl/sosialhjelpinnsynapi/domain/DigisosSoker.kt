package no.nav.sbl.sosialhjelpinnsynapi.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InternalDigisosSoker(
        var referanse: String?,
        var status: SoknadsStatus?,
        var saker: MutableCollection<Sak>,
        var forvaltningsbrev: MutableCollection<Forvaltningsbrev>,
        var soknadsmottaker: Soknadsmottaker?,
        var historikk: MutableCollection<Hendelse>
) {
    constructor() : this(null, null, mutableListOf(), mutableListOf(), null, mutableListOf())
}

data class Forvaltningsbrev(
        var referanse: String,
        var tittel: String
)

data class Soknadsmottaker(
        val navEnhetsnummer: String,
        val navEnhetsnavn: String
)

data class Sak(
        var referanse: String,
        var saksStatus: SaksStatus,
        var tittel: String,
        var vedtak: MutableCollection<Vedtak>,
        var utbetalinger: MutableCollection<Utbetaling>
)

data class Vedtak(
        var utfall: UtfallVedtak,
        var vedtaksFilUrl: String
)

data class Utbetaling(
        var referanse: String,
        var status: UtbetalingsStatus,
        var belop: BigDecimal,
        var beskrivelse: String,
        var posteringsDato: LocalDate,
        var fom: LocalDate,
        var tom: LocalDate,
        var mottaker: String,
        var utbetalingsform: String
)

data class Vilkar(
        var referanse: String,
        var utbetalinger: MutableCollection<Utbetaling>,
        var beskrivelse: String
)

data class Hendelse(
        val tittel: String,
        val tidspunkt: LocalDateTime,
        val url: String?
) {
    constructor(tittel: String, tidspunkt: LocalDateTime) : this(tittel, tidspunkt, null)
}

enum class SoknadsStatus {
    SENDT, MOTTATT, UNDER_BEHANDLING, FERDIGBEHANDLET
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