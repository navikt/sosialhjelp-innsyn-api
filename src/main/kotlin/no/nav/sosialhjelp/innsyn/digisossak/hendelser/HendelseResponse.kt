package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import no.nav.sosialhjelp.innsyn.domain.UrlResponse

data class HendelseResponse(
    val tidspunkt: String,
    val beskrivelse: String,
    val filUrl: UrlResponse?
)
