package no.nav.sosialhjelp.innsyn.digisossak.sak

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import java.time.LocalDate

data class SakResponse(
    val tittel: String,
    val vedtaksfilUrlList: List<FilUrl>?,
    val utfallVedtak: UtfallVedtak?,
    val navEnhetNavn: String?,
)

data class FilUrl(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate?,
    val url: String,
    val id: String,
)
