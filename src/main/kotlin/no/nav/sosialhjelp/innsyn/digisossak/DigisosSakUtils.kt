package no.nav.sosialhjelp.innsyn.digisossak

import no.nav.sosialhjelp.api.fiks.DigisosSak
import java.time.LocalDateTime
import java.time.ZoneOffset

fun DigisosSak.isNewerThanMonths(months: Int): Boolean {
    val testDato = LocalDateTime.now().minusMonths(months.toLong()).toInstant(ZoneOffset.UTC).toEpochMilli()
    return sistEndret >= testDato
}
