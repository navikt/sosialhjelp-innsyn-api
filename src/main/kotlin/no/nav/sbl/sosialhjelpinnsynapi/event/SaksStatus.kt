package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonSaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonSaksStatus) {

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.referanse }

    if (sakForReferanse != null) {
        // Oppdater felter

        if (hendelse.status != null) {
            val prevStatus = sakForReferanse.saksStatus

            sakForReferanse.saksStatus = SaksStatus.valueOf(hendelse.status?.name
                    ?: JsonSaksStatus.Status.UNDER_BEHANDLING.name)

            if (prevStatus != sakForReferanse.saksStatus
                    && (sakForReferanse.saksStatus == SaksStatus.IKKE_INNSYN || sakForReferanse.saksStatus == SaksStatus.BEHANDLES_IKKE)) {
                historikk.add(Hendelse("Vi kan ikke vise behandlingsstatus for ${hendelse.tittel} digitalt.", toLocalDateTime(hendelse.hendelsestidspunkt)))
            }
        }

        sakForReferanse.tittel = hendelse.tittel

    } else {
        // Opprett ny Sak
        val status = SaksStatus.valueOf(hendelse.status?.name ?: JsonSaksStatus.Status.UNDER_BEHANDLING.name)
        saker.add(Sak(
                hendelse.referanse,
                status,
                hendelse.tittel,
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
        ))
        val tittel = hendelse.tittel ?: "Saken"
        val beskrivelse: String? = when (status) {
            SaksStatus.UNDER_BEHANDLING -> "$tittel er under behandling"
            SaksStatus.BEHANDLES_IKKE, SaksStatus.IKKE_INNSYN -> "Vi kan ikke vise behandlingsstatus for $tittel digitalt."
            else -> null
        }
        if (beskrivelse != null) {
            historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt)))
        }
    }
}
