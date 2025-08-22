package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import java.time.LocalDateTime

data class OriginalSoknadDto(
    val url: String,
    val size: Long? = null,
    val filename: String? = null,
    val date: LocalDateTime? = null,
)
