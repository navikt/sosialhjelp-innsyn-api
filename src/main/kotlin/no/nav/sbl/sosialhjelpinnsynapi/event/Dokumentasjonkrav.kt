package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonkrav) {

    val utbetalingerMedSakKnytning = mutableListOf<Utbetaling>()
    val utbetalingerUtenSakKnytning = mutableListOf<Utbetaling>()
    val dokumentasjonkravsaker = mutableListOf<Sak>()
    for (utbetalingsreferanse in hendelse.utbetalingsreferanse) {
        // utbetalinger knyttet til sak
        for (sak in saker) {
            for (utbetaling in sak.utbetalinger) {
                if (utbetaling.referanse == utbetalingsreferanse) {
                    utbetalingerMedSakKnytning.add(utbetaling)
                }
            }
        }
        // utbetalinger ikke knyttet til sak
        for (utbetalingUtenSak in utbetalinger) {
            if (utbetalingUtenSak.referanse == utbetalingsreferanse) {
                utbetalingerUtenSakKnytning.add(utbetalingUtenSak)
            }

        }
    }

    val dokumentasjonkrav = Dokumentasjonkrav(
            referanse = hendelse.dokumentasjonkravreferanse,
            utbetalinger = utbetalingerMedSakKnytning.union(utbetalingerUtenSakKnytning).toMutableList(),
            beskrivelse = hendelse.beskrivelse,
            oppfyllt = hendelse.status == JsonDokumentasjonkrav.Status.OPPFYLT
    )

    // todo: er status OPPFYLT vs IKKE_OPPFYLT viktig i denne sammenhengen?
    val beskrivelse = "Dine dokumentasjonskrav har blitt oppdatert, les vedtaket for detaljer hva du må sende inn."
    dokumentasjonkravsaker.forEach { sak ->
        sak.dokumentasjonkrav.add(dokumentasjonkrav)
        if (sak.saksStatus == SaksStatus.FERDIGBEHANDLET) {
            historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt)))
        }
    }

    utbetalinger.forEach { utbetaling ->
        utbetaling.dokumentasjonkrav.add(dokumentasjonkrav)
        // todo: finn ut om dette skal med
        if (status == SoknadsStatus.FERDIGBEHANDLET) {
            historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt)))
        }
    }
}