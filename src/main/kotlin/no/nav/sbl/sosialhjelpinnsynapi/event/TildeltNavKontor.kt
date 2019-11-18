package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sbl.sosialhjelpinnsynapi.common.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonTildeltNavKontor, norgClient: NorgClient) {

    if (hendelse.navKontor == soknadsmottaker?.navEnhetsnummer) {
        return
    }
    val detalj = try {
        val navKontorNavn = norgClient.hentNavEnhet(hendelse.navKontor).navn
        navKontorNavn
    } catch (e: NorgException) {
        "et annet NAV-kontor"
    }
    val beskrivelse = "Søknaden med vedlegg er videresendt og mottatt ved $detalj. Videresendingen vil ikke påvirke saksbehandlingstiden."
    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt)))
}