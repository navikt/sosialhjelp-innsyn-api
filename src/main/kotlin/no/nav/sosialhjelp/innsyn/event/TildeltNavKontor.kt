package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sosialhjelp.innsyn.app.exceptions.NorgException
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HistorikkType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Soknadsmottaker
import no.nav.sosialhjelp.innsyn.navenhet.NorgClient
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonTildeltNavKontor::class.java.name)

fun InternalDigisosSoker.apply(
    hendelse: JsonTildeltNavKontor,
    norgClient: NorgClient,
) {

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
    soknadsmottaker = Soknadsmottaker(hendelse.navKontor, destinasjon)

    val isFirstTimeTildeltNavKontor = historikk.none { it.type == HistorikkType.TILDELT_NAV_KONTOR }
    // Ikke si at søknaden er videresendt hvis søknaden er en papirsøknad (tidspunktSendt == null)
    // og det er første gang den er tildelt et nav-kontor
    val beskrivelse =
        if (tidspunktSendt == null && isFirstTimeTildeltNavKontor) {
            "Din søknad er mottatt ved $destinasjon"
        } else {
            "Søknaden med vedlegg er videresendt og mottatt ved $destinasjon. Videresendingen vil ikke påvirke saksbehandlingstiden."
        }

    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Tildelt Navkontor. Beskrivelse: $beskrivelse")
    historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), type = HistorikkType.TILDELT_NAV_KONTOR))
}
