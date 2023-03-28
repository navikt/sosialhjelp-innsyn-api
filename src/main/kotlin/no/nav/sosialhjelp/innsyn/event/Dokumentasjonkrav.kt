package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonkrav
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HistorikkType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonDokumentasjonkrav::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonkrav) {
    val dokumentasjonkrav = Dokumentasjonkrav(
        dokumentasjonkravId = sha256(hendelse.frist?.toLocalDateTime()?.toLocalDate().toString()),
        hendelsetype = JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
        referanse = hendelse.dokumentasjonkravreferanse,
        tittel = hendelse.tittel,
        beskrivelse = hendelse.beskrivelse,
        status = Oppgavestatus.valueOf(hendelse.status.value()),
        datoLagtTil = hendelse.hendelsestidspunkt.toLocalDateTime(),
        frist = hendelse.frist?.toLocalDateTime()?.toLocalDate(),
        utbetalingsReferanse = hendelse.utbetalingsreferanse
    )

    this.dokumentasjonkrav.oppdaterEllerLeggTilDokumentasjonkrav(dokumentasjonkrav)

    val utbetalingerMedSakKnytning = saker.flatMap { it.utbetalinger }.filter { it.referanse in hendelse.utbetalingsreferanse }
    val utbetalingerUtenSakKnytning = utbetalinger.filter { it.referanse in hendelse.utbetalingsreferanse }

    if (status == SoknadsStatus.FERDIGBEHANDLET) {
        log.warn("Dokumentasjonkrav lagt til etter at søknad er satt til ferdigbehandlet. fiksDigisosId: $fiksDigisosId")
    }
    if (utbetalingerMedSakKnytning.isEmpty() && utbetalingerUtenSakKnytning.isEmpty()) {
        log.warn("Fant ingen utbetalinger å knytte dokumentasjonkrav til. Utbetalingsreferanser: ${hendelse.utbetalingsreferanse}")
        return
    }

    val union = utbetalingerMedSakKnytning.union(utbetalingerUtenSakKnytning)
    union.forEach { it.dokumentasjonkrav.oppdaterEllerLeggTilDokumentasjonkrav(dokumentasjonkrav) }

    val beskrivelse = "Dine oppgaver er oppdatert, les mer i vedtaket."
    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Dokumentasjonskrav. Beskrivelse: $beskrivelse")
    historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), type = HistorikkType.DOKUMENTASJONSKRAV))
}

private fun MutableList<Dokumentasjonkrav>.oppdaterEllerLeggTilDokumentasjonkrav(dokumentasjonkrav: Dokumentasjonkrav) {
    if (any { it.referanse == dokumentasjonkrav.referanse }) {
        filter { it.referanse == dokumentasjonkrav.referanse }
            .forEach { it.oppdaterFelter(dokumentasjonkrav.beskrivelse) }
    } else {
        this.add(dokumentasjonkrav)
    }
}

private fun Dokumentasjonkrav.oppdaterFelter(beskrivelse: String?) {
    this.beskrivelse = beskrivelse
}
