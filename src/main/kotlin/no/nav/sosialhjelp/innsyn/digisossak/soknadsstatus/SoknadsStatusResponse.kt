package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import java.time.LocalDateTime

data class SoknadsStatusResponse(
    val status: SoknadsStatus,
    val kommunenummer: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val tidspunktSendt: LocalDateTime?,
    val soknadsalderIMinutter: Long?,
    val navKontor: String?,
    val filUrl: UrlResponse?,
)
