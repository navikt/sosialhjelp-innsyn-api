package no.nav.sosialhjelp.innsyn.digisossak.sak

import java.time.LocalDate

data class SakResponse(
    val tittel: String,
    val vedtaksdato: LocalDate,
    val vedtaksBrev: FilUrl,
    val navEnhetNavn: String?,
    val soknadSendtDato: LocalDate?,
)

data class FilUrl(
    val url: String,
    val id: String,
)
