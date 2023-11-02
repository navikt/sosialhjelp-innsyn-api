package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import java.time.LocalDate

data class DokumentasjonkravResponse(
    val dokumentasjonkravElementer: List<DokumentasjonkravElement>,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val frist: LocalDate?,
    val dokumentasjonkravId: String,
)

data class DokumentasjonkravElement(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val hendelsetidspunkt: LocalDate,
    val hendelsetype: JsonVedlegg.HendelseType?,
    // hendelsereferanse
    val dokumentasjonkravReferanse: String,
    val tittel: String?,
    val beskrivelse: String?,
    val status: Oppgavestatus,
    val utbetalingsReferanse: List<String>?,
)
