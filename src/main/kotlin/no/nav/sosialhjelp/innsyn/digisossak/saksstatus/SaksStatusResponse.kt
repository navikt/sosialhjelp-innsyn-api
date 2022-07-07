package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import java.time.LocalDate

data class SaksStatusResponse(
    val tittel: String,
    val status: SaksStatus?,
    val skalViseVedtakInfoPanel: Boolean,
    val vedtaksfilUrlList: List<VedtaksfilUrl>?
)

data class VedtaksfilUrl(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    val vedtaksfilUrl: String
)
