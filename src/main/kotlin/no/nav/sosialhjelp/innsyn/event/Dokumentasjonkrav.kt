package no.nav.sosialhjelp.innsyn.event

import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sosialhjelp.innsyn.client.unleash.DOKUMENTASJONKRAV_ENABLED
import no.nav.sosialhjelp.innsyn.domain.*
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonkrav, unleashClient: Unleash) {

    val log by logger()

    val utbetalingerMedSakKnytning = mutableListOf<Utbetaling>()
    val utbetalingerUtenSakKnytning = mutableListOf<Utbetaling>()
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

    if (utbetalingerMedSakKnytning.isEmpty() && utbetalingerUtenSakKnytning.isEmpty()) {
        log.warn("Fant ingen utbetalinger å knytte dokumentasjonkrav til. Utbetalingsreferanser: ${hendelse.utbetalingsreferanse}")
        return
    }

    val dokumentasjonkrav = Dokumentasjonkrav(
            referanse = hendelse.dokumentasjonkravreferanse,
            tittel = hendelse.tittel,
            beskrivelse = hendelse.beskrivelse,
            oppfyllt = hendelse.status == JsonDokumentasjonkrav.Status.OPPFYLT,
            datoLagtTil = hendelse.hendelsestidspunkt.toLocalDateTime(),
    )

    val union = utbetalingerMedSakKnytning.union(utbetalingerUtenSakKnytning)
    union.forEach { it.dokumentasjonkrav.oppdaterEllerLeggTilDokumentasjonkrav(hendelse, dokumentasjonkrav) }

    if (unleashClient.isEnabled(DOKUMENTASJONKRAV_ENABLED, false)) {
        val beskrivelse = "Dokumentasjonskravene dine er oppdatert, les mer i vedtaket."
        historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime()))
    }
}

private fun MutableList<Dokumentasjonkrav>.oppdaterEllerLeggTilDokumentasjonkrav(hendelse: JsonDokumentasjonkrav, dokumentasjonkrav: Dokumentasjonkrav) {
    if (any { it.referanse == hendelse.dokumentasjonkravreferanse }) {
        filter { it.referanse == hendelse.dokumentasjonkravreferanse }
                .forEach { it.oppdaterFelter(hendelse) }
    } else {
        this.add(dokumentasjonkrav)
    }
}

private fun Dokumentasjonkrav.oppdaterFelter(hendelse: JsonDokumentasjonkrav) {
    beskrivelse = hendelse.beskrivelse
    oppfyllt = hendelse.status == JsonDokumentasjonkrav.Status.OPPFYLT
}