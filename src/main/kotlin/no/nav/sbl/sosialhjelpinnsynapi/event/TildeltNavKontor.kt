package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.common.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.utils.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonTildeltNavKontor, norgClient: NorgClient) {

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
    historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime()))
}