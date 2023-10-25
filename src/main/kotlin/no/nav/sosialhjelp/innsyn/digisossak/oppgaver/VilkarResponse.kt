package no.nav.sosialhjelp.innsyn.digisossak.oppgaver

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import java.time.LocalDate

data class VilkarResponse(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val hendelsetidspunkt: LocalDate,
    val vilkarReferanse: String,
    val tittel: String?,
    val beskrivelse: String?,
    val status: Oppgavestatus,
    val utbetalingsReferanse: List<String>?,
)
