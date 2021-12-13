package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonRammevedtak
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonRammevedtak::class.java.name)

fun apply(hendelse: JsonRammevedtak) {
    log.info("Hendelse: Rammevedtak. Vi viser ikke rammevedtak for bruker.")
    // lar stå som blank inntil videre
}
