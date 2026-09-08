package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveResponseBeta(
    val oppgaveId: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
    val dokumenttype: String,
    val tilleggsinformasjon: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val erFraInnsyn: Boolean,
    val erLastetOpp: Boolean,
    val opplastetDato: LocalDateTime?,
)

data class OppgaveVedleggFil(
    val url: String,
    val filnavn: String,
    val storrelse: Long,
    val tidspunktLastetOpp: LocalDateTime,
)
