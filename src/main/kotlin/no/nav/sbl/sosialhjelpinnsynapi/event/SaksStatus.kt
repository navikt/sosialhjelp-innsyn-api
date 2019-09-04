package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus

fun InternalDigisosSoker.apply(hendelse: JsonSaksStatus) {

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.referanse }

    if (sakForReferanse != null) {
        // Oppdater felter
        sakForReferanse.saksStatus = SaksStatus.valueOf(hendelse.status.name)
        sakForReferanse.tittel = hendelse.tittel
    } else {
        // Opprett ny Sak
        saker.add(Sak(
                hendelse.referanse,
                SaksStatus.valueOf(hendelse.status.name),
                hendelse.tittel,
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
        ))
    }
}
