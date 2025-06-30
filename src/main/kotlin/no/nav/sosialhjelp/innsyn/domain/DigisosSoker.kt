package no.nav.sosialhjelp.innsyn.domain

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.HendelseDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class InternalDigisosSoker(
    var fagsystem: Fagsystem? = null,
    var referanse: String? = null,
    var fiksDigisosId: String? = null,
    var status: SoknadsStatus = SoknadsStatus.SENDT,
    var saker: MutableList<Sak> = mutableListOf(),
    var utbetalinger: MutableList<Utbetaling> = mutableListOf(),
    var forvaltningsbrev: MutableList<Forvaltningsbrev> = mutableListOf(),
    var soknadsmottaker: Soknadsmottaker? = null,
    var tildeltNavKontor: String? = null,
    var oppgaver: MutableList<Oppgave> = mutableListOf(),
    var historikk: MutableList<Hendelse> = mutableListOf(),
    var forelopigSvar: ForelopigSvar = ForelopigSvar(false, null),
    var tidspunktSendt: LocalDateTime? = null,
    var vilkar: MutableList<Vilkar> = mutableListOf(),
    var dokumentasjonkrav: MutableList<Dokumentasjonkrav> = mutableListOf(),
)

data class Fagsystem(
    var systemnavn: String?,
    var systemversjon: String?,
)

data class Forvaltningsbrev(
    var referanse: String,
    var tittel: String,
)

data class Soknadsmottaker(
    val navEnhetsnummer: String?,
    val navEnhetsnavn: String?,
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
    val id: String,
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
    var stoppetDato: LocalDate?,
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

    fun getOppgaveStatus(): Oppgavestatus =
        when (status) {
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
    var datoSistEndret: LocalDateTime,
) : Oppgavehendelse()

data class Dokumentasjonkrav(
    val dokumentasjonkravId: String,
    val hendelsetype: JsonVedlegg.HendelseType?,
    // hendelsereferanse
    override var referanse: String,
    override var tittel: String?,
    override var beskrivelse: String?,
    override var status: Oppgavestatus,
    override var utbetalingsReferanse: List<String>?,
    var datoLagtTil: LocalDateTime,
    var frist: LocalDate?,
) : Oppgavehendelse()

data class Hendelse(
    val hendelseType: HendelseTekstType,
    val tidspunkt: LocalDateTime,
    val url: UrlResponse? = null,
    val type: HistorikkType? = null,
    val tekstArgument: String? = null,
    val saksReferanse: String? = null,
)

data class UrlResponse(
    val linkTekst: HendelseTekstType,
    val link: String,
)

data class ForelopigSvar(
    val harMottattForelopigSvar: Boolean,
    val link: String?,
)

enum class SoknadsStatus {
    SENDT,
    MOTTATT,
    UNDER_BEHANDLING,
    FERDIGBEHANDLET,
    BEHANDLES_IKKE,
}

enum class SaksStatus {
    UNDER_BEHANDLING,
    IKKE_INNSYN,
    FERDIGBEHANDLET,
    BEHANDLES_IKKE,
    FEILREGISTRERT,
}

enum class UtbetalingsStatus {
    PLANLAGT_UTBETALING,
    UTBETALT,
    STOPPET,
    ANNULLERT,
}

enum class UtfallVedtak {
    INNVILGET,
    DELVIS_INNVILGET,
    AVSLATT,
    AVVIST,
}

enum class Oppgavestatus {
    RELEVANT,
    ANNULLERT,
    OPPFYLT,
    IKKE_OPPFYLT,
    LEVERT_TIDLIGERE,
}

enum class HistorikkType {
    TILDELT_NAV_KONTOR,
    DOKUMENTASJONSKRAV,
}

interface HendelseDtoMapper {
    fun mapTilHendelseDto(
        hendelse: Hendelse,
        enhetNavn: String?,
    ): HendelseDto?
}

enum class HendelseTekstType : HendelseDtoMapper {
    SOKNAD_SEND_TIL_KONTOR {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Sendt(hendelse.tidspunkt, enhetNavn, hendelse.url?.link)
    },
    SOKNAD_UNDER_BEHANDLING {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SoknadUnderBehandling(hendelse.tidspunkt, enhetNavn)
    },
    SOKNAD_MOTTATT_MED_KOMMUNENAVN {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Mottatt(hendelse.tidspunkt, enhetNavn)
    },
    SOKNAD_MOTTATT_UTEN_KOMMUNENAVN {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Mottatt(hendelse.tidspunkt, enhetNavn)
    },
    SOKNAD_FERDIGBEHANDLET {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SoknadFerdigBehandlet(hendelse.tidspunkt, hendelse.url?.link)
    },
    SOKNAD_BEHANDLES_IKKE {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.BehandlesIkke(hendelse.tidspunkt)
    },
    SOKNAD_VIDERESENDT_PAPIRSOKNAD_MED_NORG_ENHET {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Videresendt(hendelse.tidspunkt, hendelse.tekstArgument, true)
    },
    SOKNAD_VIDERESENDT_PAPIRSOKNAD_UTEN_NORG_ENHET {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Videresendt(hendelse.tidspunkt, hendelse.tekstArgument, true)
    },
    SOKNAD_VIDERESENDT_MED_NORG_ENHET {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Videresendt(hendelse.tidspunkt, hendelse.tekstArgument, false)
    },
    SOKNAD_VIDERESENDT_UTEN_NORG_ENHET {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.Videresendt(hendelse.tidspunkt, hendelse.tekstArgument, false)
    },
    SOKNAD_KAN_IKKE_VISE_STATUS_MED_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SoknadKanIkkeViseStatus(hendelse.tidspunkt, hendelse.tekstArgument)
    },
    SOKNAD_KAN_IKKE_VISE_STATUS_UTEN_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SoknadKanIkkeViseStatus(hendelse.tidspunkt, hendelse.tekstArgument)
    },
    SAK_UNDER_BEHANDLING_MED_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SakUnderBehandling(hendelse.tidspunkt, hendelse.tekstArgument)
    },
    SAK_UNDER_BEHANDLING_UTEN_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SakUnderBehandling(hendelse.tidspunkt, hendelse.tekstArgument)
    },
    SAK_FERDIGBEHANDLET_MED_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SakFerdigBehandlet(hendelse.tidspunkt, hendelse.tekstArgument, hendelse.url?.link)
    },
    SAK_FERDIGBEHANDLET_UTEN_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SakFerdigBehandlet(hendelse.tidspunkt, hendelse.tekstArgument, hendelse.url?.link)
    },
    SAK_KAN_IKKE_VISE_STATUS_MED_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SakKanIkkeViseStatus(hendelse.tidspunkt, hendelse.tekstArgument)
    },
    SAK_KAN_IKKE_VISE_STATUS_UTEN_TITTEL {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.SakKanIkkeViseStatus(hendelse.tidspunkt, hendelse.tekstArgument)
    },
    ANTALL_SENDTE_VEDLEGG {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto =
            HendelseDto.LevertEtterspurtDokumentasjon(
                hendelse.tidspunkt,
                hendelse.tekstArgument?.toIntOrNull() ?: 0,
            )
    },
    UTBETALINGER_OPPDATERT {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.UtbetalingerOppdatert(hendelse.tidspunkt)
    },
    BREV_OM_SAKSBEANDLINGSTID {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.ForelopigSvarHendelse(hendelse.tidspunkt, hendelse.url?.link)
    },
    ETTERSPOR_MER_DOKUMENTASJON {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.EtterspurtDokumentasjon(hendelse.tidspunkt, hendelse.url?.link)
    },
    ETTERSPOR_IKKE_MER_DOKUMENTASJON {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.LevertEtterspurtDokumentasjon(hendelse.tidspunkt, 0)
    },
    DOKUMENTASJONKRAV {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto = HendelseDto.DokumentasjonKrav(hendelse.tidspunkt, hendelse.url?.link)
    },
    SOKNAD_SEND_TIL_KONTOR_LENKETEKST {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto? = null
    },
    VIS_BREVET_LENKETEKST {
        override fun mapTilHendelseDto(
            hendelse: Hendelse,
            enhetNavn: String?,
        ): HendelseDto? = null
    },
}
