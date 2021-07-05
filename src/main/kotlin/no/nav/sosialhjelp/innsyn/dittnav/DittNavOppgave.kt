package no.nav.sosialhjelp.innsyn.dittnav

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class DittNavOppgave(
    val eventId: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val eventTidspunkt: LocalDateTime,
    val grupperingsId: String,
    val tekst: String,
    val link: String,
    val sikkerhetsnivaa: Int,
    val aktiv: Boolean
)
