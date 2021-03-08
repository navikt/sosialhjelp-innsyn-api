package no.nav.sosialhjelp.innsyn.domain

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVilkar
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

data class SoknadsStatusResponse(
        val status: SoknadsStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        val tidspunktSendt: LocalDateTime?,
        val soknadsalderIMinutter: Long?,
        val navKontor: String?,
        val filUrl: UrlResponse?
)

data class SaksStatusResponse(
        val tittel: String,
        val status: SaksStatus?,
        val skalViseVedtakInfoPanel: Boolean,
        val vedtaksfilUrlList: List<VedtaksfilUrl>?
)

data class VedtaksfilUrl(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val dato: LocalDate?,
        val vedtaksfilUrl: String
)

data class HendelseResponse(
        val tidspunkt: String,
        val beskrivelse: String,
        val filUrl: UrlResponse?
)

data class OppgaveResponse(
        val oppgaveId: String,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val innsendelsesfrist: LocalDate?,
        val oppgaveElementer: List<OppgaveElement>
)

data class OppgaveElement(
        val dokumenttype: String,
        val tilleggsinformasjon: String?,
        val hendelsetype: JsonVedlegg.HendelseType?,
        val hendelsereferanse: String?,
        val erFraInnsyn: Boolean
)

data class VilkarResponse(
        val vilkarElementer: List<VilkarElement>
)

data class VilkarElement(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val hendelsetidspunkt: LocalDate,
        val vilkarReferanse: String,
        val tittel: String?,
        val beskrivelse: String?
)

data class DokumentasjonkravResponse(
        val dokumentasjonkravElementer: List<DokumentasjonkravElement>
)

data class DokumentasjonkravElement(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val hendelsetidspunkt: LocalDate,
        val hendelsetype: JsonVedlegg.HendelseType?,
        val dokumentasjonkravReferanse : String, // hendelsereferanse
        val tittel: String?,
        val beskrivelse: String?
)


data class UtbetalingerResponse(
        val ar: Int,
        val maned: String,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val foersteIManeden: LocalDate,
        val utbetalinger: List<ManedUtbetaling>
)

data class ManedUtbetaling(
        val tittel: String,
        val belop: Double,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val utbetalingsdato: LocalDate?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val forfallsdato: LocalDate?,
        val status: String,
        val fiksDigisosId: String,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val fom: LocalDate?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val tom: LocalDate?,
        val mottaker: String?,
        val annenMottaker: Boolean,
        val kontonummer: String?,
        val utbetalingsmetode: String?
)

data class VedleggResponse(
        val filnavn: String,
        val storrelse: Long,
        val url: String,
        val type: String,
        val tilleggsinfo: String?,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        val datoLagtTil: LocalDateTime
)

data class VedleggOpplastingResponse(
        val filnavn: String?,
        val status: String
)

data class OppgaveOpplastingResponse(
        val type: String,
        val tilleggsinfo: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val innsendelsesfrist: LocalDate?,
        val hendelsetype: JsonVedlegg.HendelseType?,
        val hendelsereferanse: String?,
        val filer: List<VedleggOpplastingResponse>
)

data class SaksListeResponse(
        val fiksDigisosId: String,
        val soknadTittel: String,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        val sistOppdatert: Date,
        val kilde: String
)

data class SaksDetaljerResponse(
        val fiksDigisosId: String,
        val soknadTittel: String,
        val status: String,
        val antallNyeOppgaver: Int?
)

data class ForelopigSvarResponse(
        val harMottattForelopigSvar: Boolean,
        val link: String?
)

data class KommuneResponse(
        val erInnsynDeaktivert: Boolean,
        val erInnsynMidlertidigDeaktivert: Boolean,
        val erInnsendingEttersendelseDeaktivert: Boolean,
        val erInnsendingEttersendelseMidlertidigDeaktivert: Boolean,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
        val tidspunkt: Date,
        val kommunenummer: String?
)

data class OrginalJsonSoknadResponse(
        val jsonSoknad: JsonSoknad
)

data class OrginalSoknadPdfLinkResponse(
        val orginalSoknadPdfLink: String
)
