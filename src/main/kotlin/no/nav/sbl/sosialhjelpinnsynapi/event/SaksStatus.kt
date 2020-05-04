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

        sakForReferanse.tittel = hendelse.tittel

        if (hendelse.status != null) {
            val prevStatus = sakForReferanse.saksStatus

            sakForReferanse.saksStatus = SaksStatus.valueOf(hendelse.status?.name
                    ?: JsonSaksStatus.Status.UNDER_BEHANDLING.name)

            if (prevStatus != sakForReferanse.saksStatus
                    && (sakForReferanse.saksStatus == SaksStatus.IKKE_INNSYN || sakForReferanse.saksStatus == SaksStatus.BEHANDLES_IKKE)) {
                val tittel = sakForReferanse.tittel ?: "saken din"
                historikk.add(Hendelse("Vi kan ikke vise behandlingsstatus for $tittel digitalt.", hendelse.hendelsestidspunkt.toLocalDateTime()))
            }
        }

    } else {
        // Opprett ny Sak
        val status = SaksStatus.valueOf(hendelse.status?.name ?: JsonSaksStatus.Status.UNDER_BEHANDLING.name)
        saker.add(Sak(
                referanse = hendelse.referanse,
                saksStatus = status,
                tittel = hendelse.tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf()
        ))
        val tittel = hendelse.tittel ?: "saken din"
        val beskrivelse: String? = when (status) {
            SaksStatus.UNDER_BEHANDLING -> "${tittel.capitalize()} er under behandling"
            SaksStatus.BEHANDLES_IKKE, SaksStatus.IKKE_INNSYN -> "Vi kan ikke vise behandlingsstatus for $tittel digitalt."
            else -> null
        }
        if (beskrivelse != null) {
            historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime()))
        }
    }
}
