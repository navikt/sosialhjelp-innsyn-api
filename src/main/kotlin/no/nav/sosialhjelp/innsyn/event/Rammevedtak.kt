package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonRammevedtak
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import org.slf4j.Logger

fun InternalDigisosSoker.apply(hendelse: JsonRammevedtak, log: Logger) {
    log.info("Hendelse: Rammevedtak. Vi viser ikke rammevedtak for bruker.")
    // lar stå som blank inntil videre
}
