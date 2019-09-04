package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Dokumentasjonkrav
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Utbetaling

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonkrav, clientProperties: ClientProperties) {
    hendelse.beskrivelse
    hendelse.status
    hendelse.utbetalingsreferanse


    val utbetalinger: MutableList<Utbetaling> = mutableListOf()
    val dokumentasjonkravSaker: MutableList<Utbetaling> = mutableListOf()
    for (utbetalingsreferanse in hendelse.utbetalingsreferanse) {
        for (sak in saker) {
            for (utbetaling in sak.utbetalinger) {
                if (utbetaling.referanse == utbetalingsreferanse) {
                    utbetalinger.add(utbetaling)
                }
            }
        }
    }
    val dokumentasjonkrav = Dokumentasjonkrav(hendelse.dokumentasjonkravreferanse, utbetalinger, hendelse.beskrivelse, hendelse.status == JsonDokumentasjonkrav.Status.OPPFYLT)

    dokumentasjonkravSaker.forEach { sak ->
        sak.dokumentasjonkrav.add(dokumentasjonkrav)
    }

    utbetalinger.forEach { utbetaling ->
        utbetaling.dokumentasjonkrav.add(dokumentasjonkrav)
    }

}