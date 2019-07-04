package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus

fun InternalDigisosSoker.applySaksStatus(hendelse: JsonSaksStatus) {

    // TODO: håndter historikk

    val filtered = saker.filter { it.referanse == hendelse.referanse }
    if (filtered.isNotEmpty() && filtered.size == 1) {
        // Oppdater felter som _kan_ oppdateres
        val existingSak = filtered[0]
        existingSak.saksStatus = SaksStatus.valueOf(hendelse.status.name)
        existingSak.tittel = hendelse.tittel
    } else {
        saker.add(Sak(
                hendelse.referanse,
                SaksStatus.valueOf(hendelse.status.name),
                hendelse.tittel,
                mutableListOf(),
                mutableListOf()
        ))
    }

}
