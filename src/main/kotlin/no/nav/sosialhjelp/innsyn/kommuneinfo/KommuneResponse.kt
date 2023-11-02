package no.nav.sosialhjelp.innsyn.kommuneinfo

import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

data class KommuneResponse(
    val erInnsynDeaktivert: Boolean,
    val erInnsynMidlertidigDeaktivert: Boolean,
    val erInnsendingEttersendelseDeaktivert: Boolean,
    val erInnsendingEttersendelseMidlertidigDeaktivert: Boolean,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val tidspunkt: Date,
    val kommunenummer: String?,
)
