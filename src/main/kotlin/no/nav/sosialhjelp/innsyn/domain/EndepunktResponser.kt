package no.nav.sosialhjelp.innsyn.domain

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

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
