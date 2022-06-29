package no.nav.sosialhjelp.innsyn.vedlegg.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import java.time.LocalDate

data class OppgaveOpplastingResponse(
    val type: String,
    val tilleggsinfo: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: List<VedleggOpplastingResponse>
)
