package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVilkar
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.Utbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.Vilkar
import no.nav.sbl.sosialhjelpinnsynapi.hendelse.HendelseService.Companion.log
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonVilkar) {

    val utbetalinger = mutableListOf<Utbetaling>()
    val vilkarSaker = mutableListOf<Sak>()
    for (utbetalingsreferanse in hendelse.utbetalingsreferanse) {
        for (sak in saker) {
            for (utbetaling in sak.utbetalinger) {
                if (utbetaling.referanse == utbetalingsreferanse) {
                    utbetalinger.add(utbetaling)
                }
            }
        }
    }
    val vilkar = Vilkar(
            referanse = hendelse.vilkarreferanse,
            utbetalinger = utbetalinger,
            beskrivelse = hendelse.beskrivelse,
            oppfyllt = hendelse.status == JsonVilkar.Status.OPPFYLT,
            datoLagtTil = hendelse.hendelsestidspunkt.toLocalDateTime(),
            datoSistEndret = hendelse.hendelsestidspunkt.toLocalDateTime()
    )

    vilkarSaker.forEach { it.vilkar.oppdaterEllerLeggTilVilkar(hendelse, vilkar) }
    utbetalinger.forEach { it.vilkar.oppdaterEllerLeggTilVilkar(hendelse, vilkar) }

}

private fun MutableList<Vilkar>.oppdaterEllerLeggTilVilkar(hendelse: JsonVilkar, vilkar: Vilkar) {
    if (any { it.referanse == hendelse.vilkarreferanse }) {
        filter { it.referanse == hendelse.vilkarreferanse }
                .forEach {
                    it.oppdaterFelter(hendelse)
                    log.warn("Vilkar endret - vilkarreferanse:${hendelse.vilkarreferanse}")
                }
    } else {
        this.add(vilkar)
        log.warn("Nytt Vilkar lagt til - vilkarreferanse:${hendelse.vilkarreferanse}")
    }
}

private fun Vilkar.oppdaterFelter(hendelse: JsonVilkar) {
    datoSistEndret = hendelse.hendelsestidspunkt.toLocalDateTime()
    beskrivelse = hendelse.beskrivelse
    oppfyllt = hendelse.status == JsonVilkar.Status.OPPFYLT
}
