package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVilkar
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Utbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.Vilkar

fun InternalDigisosSoker.apply(hendelse: JsonVilkar, clientProperties: ClientProperties) {
    hendelse.beskrivelse
    hendelse.status
    hendelse.utbetalingsreferanse


    val utbetalinger: MutableList<Utbetaling> = mutableListOf()
    val vilkarSaker: MutableList<Utbetaling> = mutableListOf()


    for (utbetalingsreferanse in hendelse.utbetalingsreferanse) {
        for (sak in saker) {
            for (utbetaling in sak.utbetalinger) {
                if (utbetaling.referanse == utbetalingsreferanse) {
                    utbetalinger.add(utbetaling)
                }
            }
        }
    }
    val vilkar = Vilkar("hendelse.referanse", utbetalinger, hendelse.beskrivelse, hendelse.status == JsonVilkar.Status.OPPFYLT)

    vilkarSaker.forEach { sak ->
        sak.vilkar.add(vilkar)
    }

    utbetalinger.forEach { utbetaling ->
        utbetaling.vilkar.add(vilkar)
    }

}