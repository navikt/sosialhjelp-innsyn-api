package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.applyTildeltNavKontor(hendelse: JsonTildeltNavKontor) {

    // TODO: tildeltNavKontor berører kun historikk?

    if (hendelse.navKontor == soknadsmottaker?.navEnhetsnummer) {
        return
    }

    val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt hos ${hendelse.navKontor}"
    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt)))

}