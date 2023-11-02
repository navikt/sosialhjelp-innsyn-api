package no.nav.sosialhjelp.innsyn.vedlegg.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class VedleggResponse(
    val filnavn: String,
    val storrelse: Long,
    val url: String,
    val type: String,
    val tilleggsinfo: String?,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    val datoLagtTil: LocalDateTime,
)
