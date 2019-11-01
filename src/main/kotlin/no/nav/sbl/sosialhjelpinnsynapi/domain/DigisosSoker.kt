package no.nav.sbl.sosialhjelpinnsynapi.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InternalDigisosSoker(
        var referanse: String?,
        var status: SoknadsStatus?,
        var saker: MutableList<Sak>,
        var forvaltningsbrev: MutableList<Forvaltningsbrev>,
        var soknadsmottaker: Soknadsmottaker?,
        var oppgaver: MutableList<Oppgave>,
        var historikk: MutableList<Hendelse>
) {
    constructor() : this(null, null, mutableListOf(), mutableListOf(), null, mutableListOf(), mutableListOf())
}

data class Forvaltningsbrev(
        var referanse: String,
        var tittel: String
)

data class Soknadsmottaker(
        val navEnhetsnummer: String,
        val navEnhetsnavn: String
)

data class Oppgave(
        var tittel: String,
        var tilleggsinfo: String?,
        var innsendelsesfrist: LocalDateTime?,
        var tidspunktForKrav: LocalDateTime,
        var erFraInnsyn: Boolean
)

data class Sak(
        var referanse: String,
        var saksStatus: SaksStatus?,
        var tittel: String?,
        var vedtak: MutableList<Vedtak>,
        var utbetalinger: MutableList<Utbetaling>,
        var vilkar: MutableList<Vilkar>,
        var dokumentasjonkrav: MutableList<Dokumentasjonkrav>
)

data class Vedtak(
        var utfall: UtfallVedtak?,
        var vedtaksFilUrl: String,
        var dato: LocalDate?
)

data class Utbetaling(
        var referanse: String,
        var status: UtbetalingsStatus,
        var belop: BigDecimal,
        var beskrivelse: String?,
        var posteringsDato: LocalDate?,
        var utbetalingsDato: LocalDate?,
        var fom: LocalDate?,
        var tom: LocalDate?,
        var mottaker: String?,
        var utbetalingsform: String?,
        var vilkar: MutableList<Vilkar>,
        var dokumentasjonkrav: MutableList<Dokumentasjonkrav>
)

data class Vilkar(
        var referanse: String,
        var utbetalinger: MutableList<Utbetaling>,
        var beskrivelse: String?,
        var oppfyllt: Boolean
)

data class Dokumentasjonkrav(
        var referanse: String,
        var utbetalinger: MutableList<Utbetaling>,
        var beskrivelse: String?,
        var oppfyllt: Boolean
)

data class Hendelse(
        // type som felt?
        val tittel: String,
        val tidspunkt: LocalDateTime,
        val url: String?
) {
    constructor(tittel: String, tidspunkt: LocalDateTime) : this(tittel, tidspunkt, null)
}

enum class SoknadsStatus {
    SENDT, MOTTATT, UNDER_BEHANDLING, FERDIGBEHANDLET, BEHANDLES_IKKE
}

enum class SaksStatus {
    UNDER_BEHANDLING, IKKE_INNSYN, FERDIGBEHANDLET, BEHANDLES_IKKE, FEILREGISTRERT
}

enum class UtbetalingsStatus {
    PLANLAGT_UTBETALING, UTBETALT, STOPPET
}

enum class UtfallVedtak {
    INNVILGET, DELVIS_INNVILGET, AVSLATT, AVVIST
}