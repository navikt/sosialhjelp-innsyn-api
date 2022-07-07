package no.nav.sosialhjelp.innsyn.domain

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.time.LocalDate
import java.util.Date

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
    @JsonFormat(pattern = "yyyy-MM-dd")
    val hendelsetidspunkt: LocalDate,
    val vilkarReferanse: String,
    val tittel: String?,
    val beskrivelse: String?,
    val status: Oppgavestatus,
    val utbetalingsReferanse: List<String>?
)

data class DokumentasjonkravResponse(
    val dokumentasjonkravElementer: List<DokumentasjonkravElement>,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val frist: LocalDate?,
    val dokumentasjonkravId: String
)

data class DokumentasjonkravElement(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val hendelsetidspunkt: LocalDate,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val dokumentasjonkravReferanse: String, // hendelsereferanse
    val tittel: String?,
    val beskrivelse: String?,
    val status: Oppgavestatus,
    val utbetalingsReferanse: List<String>?
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

data class OrginalJsonSoknadResponse(
    val jsonSoknad: JsonSoknad
)

data class OrginalSoknadPdfLinkResponse(
    val orginalSoknadPdfLink: String
)
