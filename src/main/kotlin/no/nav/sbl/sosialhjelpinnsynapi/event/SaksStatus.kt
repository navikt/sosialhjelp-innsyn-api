package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus

fun InternalDigisosSoker.apply(hendelse: JsonSaksStatus) {

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.referanse }

    if (sakForReferanse != null) {
        // Oppdater felter
        if (hendelse.status != null) {
            sakForReferanse.saksStatus = SaksStatus.valueOf(hendelse.status.name)
        }
        sakForReferanse.tittel = hendelse.tittel ?: sakForReferanse.tittel
    } else {
        // Opprett ny Sak
        saker.add(Sak(
                hendelse.referanse,
                if (hendelse.status != null) SaksStatus.valueOf(hendelse.status.name) else null,
                hendelse.tittel,
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
        ))
    }
}
