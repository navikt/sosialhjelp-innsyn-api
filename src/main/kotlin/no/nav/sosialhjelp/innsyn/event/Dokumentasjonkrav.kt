package no.nav.sosialhjelp.innsyn.event

import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.client.unleash.DOKUMENTASJONKRAV_ENABLED
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonDokumentasjonkrav::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonkrav, unleashClient: Unleash) {
    val dokumentasjonkrav = Dokumentasjonkrav(
        dokumentasjonkravId = sha256(hendelse.frist?.toLocalDateTime()?.toLocalDate().toString()),
        hendelsetype = JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
        referanse = hendelse.dokumentasjonkravreferanse,
        tittel = hendelse.tittel,
        beskrivelse = hendelse.beskrivelse,
        status = Oppgavestatus.valueOf(hendelse.status.value()),
        datoLagtTil = hendelse.hendelsestidspunkt.toLocalDateTime(),
        frist = hendelse.frist?.toLocalDateTime()?.toLocalDate()
    )

    this.dokumentasjonkrav.oppdaterEllerLeggTilDokumentasjonkrav(hendelse, dokumentasjonkrav)

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

    val union = utbetalingerMedSakKnytning.union(utbetalingerUtenSakKnytning)
    union.forEach { it.dokumentasjonkrav.oppdaterEllerLeggTilDokumentasjonkrav(hendelse, dokumentasjonkrav) }

    if (unleashClient.isEnabled(DOKUMENTASJONKRAV_ENABLED, false)) {
        val beskrivelse = "Dokumentasjonskravene dine er oppdatert, les mer i vedtaket."
        log.info("Hendelse: Dokumentasjonskrav. Beskrivelse: $beskrivelse")
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
}
