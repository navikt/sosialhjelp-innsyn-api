package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import no.nav.sosialhjelp.innsyn.domain.SaksStatus

data class SaksStatusResponse(
    val tittel: String,
    val status: SaksStatus?,
    val skalViseVedtakInfoPanel: Boolean,
    val vedtaksfilUrlList: List<FilUrl>?,
)

data class FilUrl(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    val url: String,
    val vedtakId: String,
)
