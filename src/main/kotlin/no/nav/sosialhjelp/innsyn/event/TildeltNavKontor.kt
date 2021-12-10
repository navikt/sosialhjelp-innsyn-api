package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sosialhjelp.innsyn.client.norg.NorgClient
import no.nav.sosialhjelp.innsyn.common.NorgException
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.Logger

fun InternalDigisosSoker.apply(hendelse: JsonTildeltNavKontor, norgClient: NorgClient, log: Logger) {

    if (hendelse.navKontor == tildeltNavKontor) {
        return
    }

    if (hendelse.navKontor == soknadsmottaker?.navEnhetsnummer) {
        tildeltNavKontor = hendelse.navKontor
        return
    }

    tildeltNavKontor = hendelse.navKontor

    val destinasjon = try {
        norgClient.hentNavEnhet(hendelse.navKontor).navn
    } catch (e: NorgException) {
        "et annet NAV-kontor"
    }
    val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt ved $destinasjon. Videresendingen vil ikke påvirke saksbehandlingstiden."
    log.info("Hendelse: Tildelt Navkontor. Beskrivelse: $beskrivelse")
    historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime()))
}
