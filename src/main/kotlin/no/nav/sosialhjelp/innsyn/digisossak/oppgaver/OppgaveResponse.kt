package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveResponse(
    val oppgaveId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
    val oppgaveElementer: List<OppgaveElement>,
)

data class OppgaveResponseBeta(
    val oppgaveId: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
    val dokumenttype: String,
    val tilleggsinformasjon: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val erFraInnsyn: Boolean,
    val erLastetOpp: Boolean,
    val opplastetDato: LocalDateTime?,
)

data class OppgaveElement(
    val dokumenttype: String,
    val tilleggsinformasjon: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val erFraInnsyn: Boolean,
)
