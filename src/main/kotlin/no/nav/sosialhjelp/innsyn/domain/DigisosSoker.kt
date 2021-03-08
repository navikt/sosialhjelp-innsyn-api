package no.nav.sosialhjelp.innsyn.domain

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InternalDigisosSoker(
        var referanse: String?,
        var status: SoknadsStatus?,
        var saker: MutableList<Sak>,
        var utbetalinger: MutableList<Utbetaling>,
        var forvaltningsbrev: MutableList<Forvaltningsbrev>,
        var soknadsmottaker: Soknadsmottaker?,
        var tildeltNavKontor: String?,
        var oppgaver: MutableList<Oppgave>,
        var historikk: MutableList<Hendelse>,
        var forelopigSvar: ForelopigSvar,
        var tidspunktSendt: LocalDateTime?
) {
    constructor() : this(
            referanse = null,
            status = null,
            saker = mutableListOf(),
            utbetalinger = mutableListOf(),
            forvaltningsbrev = mutableListOf(),
            soknadsmottaker = null,
            tildeltNavKontor = null,
            oppgaver = mutableListOf(),
            historikk = mutableListOf(),
            forelopigSvar = ForelopigSvar(false, null),
            tidspunktSendt = null
    )
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
        var oppgaveId: String,
        var tittel: String,
        var tilleggsinfo: String?,
        val hendelsetype: JsonVedlegg.HendelseType?,
        val hendelsereferanse: String?,
        var innsendelsesfrist: LocalDateTime?,
        var tidspunktForKrav: LocalDateTime,
        var erFraInnsyn: Boolean
)

data class Sak(
        var referanse: String,
        var saksStatus: SaksStatus?,
        var tittel: String?,
        var vedtak: MutableList<Vedtak>,
        var utbetalinger: MutableList<Utbetaling>
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
        var forfallsDato: LocalDate?,
        var utbetalingsDato: LocalDate?,
        var fom: LocalDate?,
        var tom: LocalDate?,
        var mottaker: String?,
        var annenMottaker: Boolean,
        var kontonummer: String?,
        var utbetalingsmetode: String?,
        var vilkar: MutableList<Vilkar>,
        var dokumentasjonkrav: MutableList<Dokumentasjonkrav>,
        var datoHendelse: LocalDateTime
)

data class Vilkar(
        var referanse: String,
        var beskrivelse: String?,
        var oppfyllt: Boolean,
        var datoLagtTil: LocalDateTime,
        var datoSistEndret: LocalDateTime
)

data class Dokumentasjonkrav(
        var referanse: String,
        var beskrivelse: String?,
        var oppfyllt: Boolean
)

data class Hendelse(
        // type som felt?
        val tittel: String,
        val tidspunkt: LocalDateTime,
        val url: UrlResponse? = null
)

data class UrlResponse(
        val linkTekst: String,
        val link: String
)

enum class SoknadsStatus {
    SENDT, MOTTATT, UNDER_BEHANDLING, FERDIGBEHANDLET, BEHANDLES_IKKE
}

enum class SaksStatus {
    UNDER_BEHANDLING, IKKE_INNSYN, FERDIGBEHANDLET, BEHANDLES_IKKE, FEILREGISTRERT
}

enum class UtbetalingsStatus {
    PLANLAGT_UTBETALING, UTBETALT, STOPPET, ANNULLERT
}

enum class UtfallVedtak {
    INNVILGET, DELVIS_INNVILGET, AVSLATT, AVVIST
}

data class ForelopigSvar(
        val harMottattForelopigSvar: Boolean,
        val link: String?
)