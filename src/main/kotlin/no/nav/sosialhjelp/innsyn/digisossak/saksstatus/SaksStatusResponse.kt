package no.nav.sosialhjelp.innsyn.digisossak.saksstatus

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import java.time.LocalDate

data class SaksStatusResponse(
    val tittel: String,
    val status: SaksStatus?,
    val skalViseVedtakInfoPanel: Boolean,
    val vedtaksfilUrlList: List<FilUrl>?,
    val utfallVedtak: UtfallVedtak?,
    val referanse: String?,
    // TODO: Inneholder både vedtakslista og utfallet over. Kvitt oss med de to feltene når de ikke brukes mer.
    val vedtak: List<VedtakDto>,
)

data class FilUrl(
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    val url: String,
    val id: String,
)

data class VedtakDto(
    val id: String,
    var utfall: UtfallVedtak?,
    var vedtaksFilUrl: String,
    var dato: LocalDate?,
)
