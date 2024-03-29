package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import no.nav.sosialhjelp.innsyn.domain.UrlResponse

data class HendelseResponse(
    val tidspunkt: String,
    val hendelseType: String,
    val filUrl: UrlResponse?,
    val tekstArgument: String?,
)
