package no.nav.sosialhjelp.innsyn.domain

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InternalDigisosSoker(
    var avsender: Fagsystem?,
    var referanse: String?,
    var fiksDigisosId: String?,
    var status: SoknadsStatus?,
    var saker: MutableList<Sak>,
    var utbetalinger: MutableList<Utbetaling>,
    var forvaltningsbrev: MutableList<Forvaltningsbrev>,
    var soknadsmottaker: Soknadsmottaker?,
    var tildeltNavKontor: String?,
    var oppgaver: MutableList<Oppgave>,
    var historikk: MutableList<Hendelse>,
    var forelopigSvar: ForelopigSvar,
    var tidspunktSendt: LocalDateTime?,
    var vilkar: MutableList<Vilkar>,
    var dokumentasjonkrav: MutableList<Dokumentasjonkrav>,
) {

    constructor() : this(
        avsender = null,
        referanse = null,
        fiksDigisosId = null,
        status = null,
        saker = mutableListOf(),
        utbetalinger = mutableListOf(),
        forvaltningsbrev = mutableListOf(),
        soknadsmottaker = null,
        tildeltNavKontor = null,
        oppgaver = mutableListOf(),
        historikk = mutableListOf(),
        forelopigSvar = ForelopigSvar(false, null),
        tidspunktSendt = null,
        vilkar = mutableListOf(),
        dokumentasjonkrav = mutableListOf()
    )
}

data class Fagsystem(
    var systemnavn: String?,
    var systemversjon: String?,
)

data class Forvaltningsbrev(
    var referanse: String,
    var tittel: String,
)

data class Soknadsmottaker(
    val navEnhetsnummer: String,
    val navEnhetsnavn: String,
)

data class Oppgave(
    var oppgaveId: String,
    var tittel: String,
    var tilleggsinfo: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    var innsendelsesfrist: LocalDateTime?,
    var tidspunktForKrav: LocalDateTime,
    var erFraInnsyn: Boolean,
)

data class Sak(
    var referanse: String,
    var saksStatus: SaksStatus?,
    var tittel: String?,
    var vedtak: MutableList<Vedtak>,
    var utbetalinger: MutableList<Utbetaling>,
)

data class Vedtak(
    var utfall: UtfallVedtak?,
    var vedtaksFilUrl: String,
    var dato: LocalDate?,
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
    var datoHendelse: LocalDateTime,
)

// Skal renames til Hendelse eller lignende i senere refakturering
sealed class Oppgavehendelse {
    abstract var referanse: String
    abstract var tittel: String?
    abstract var beskrivelse: String?
    abstract var status: Oppgavestatus
    abstract var utbetalingsReferanse: List<String>?

    fun getTittelOgBeskrivelse(): Pair<String?, String?> {
        if (tittel.isNullOrBlank()) {
            return Pair(beskrivelse, null)
        }
        return Pair(tittel, beskrivelse)
    }

    fun getOppgaveStatus(): Oppgavestatus = when (status) {
        Oppgavestatus.OPPFYLT, Oppgavestatus.IKKE_OPPFYLT -> Oppgavestatus.RELEVANT
        else -> status
    }

    fun isEmpty(): Boolean = tittel.isNullOrBlank() && beskrivelse.isNullOrBlank()
}

data class Vilkar(
    override var referanse: String,
    override var tittel: String?,
    override var beskrivelse: String?,
    override var status: Oppgavestatus,
    override var utbetalingsReferanse: List<String>?,
    var datoLagtTil: LocalDateTime,
    var datoSistEndret: LocalDateTime
) : Oppgavehendelse()

data class Dokumentasjonkrav(
    val dokumentasjonkravId: String,
    val hendelsetype: JsonVedlegg.HendelseType?,
    override var referanse: String, // hendelsereferanse
    override var tittel: String?,
    override var beskrivelse: String?,
    override var status: Oppgavestatus,
    override var utbetalingsReferanse: List<String>?,
    var datoLagtTil: LocalDateTime,
    var frist: LocalDate?,
) : Oppgavehendelse()

data class Hendelse(
    // egentlig historikk
    // type som felt?
    val tittel: String,
    val tidspunkt: LocalDateTime,
    val url: UrlResponse? = null
)

data class UrlResponse(
    val linkTekst: String,
    val link: String
)

data class ForelopigSvar(
    val harMottattForelopigSvar: Boolean,
    val link: String?
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

enum class Oppgavestatus {
    RELEVANT, ANNULLERT, OPPFYLT, IKKE_OPPFYLT, LEVERT_TIDLIGERE
}
