package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonTildeltNavKontor
import no.nav.sosialhjelp.innsyn.app.exceptions.NorgException
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
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
    isPapirSoknad: Boolean,
) {
    if (hendelse.navKontor == tildeltNavKontor) {
        return
    }

    if (hendelse.navKontor == soknadsmottaker?.navEnhetsnummer) {
        tildeltNavKontor = hendelse.navKontor
        return
    }

    tildeltNavKontor = hendelse.navKontor

    val destinasjon =
        try {
            norgClient.hentNavEnhet(hendelse.navKontor).navn
        } catch (e: NorgException) {
            null
        }

    soknadsmottaker = Soknadsmottaker(hendelse.navKontor, destinasjon ?: "et annet NAV-kontor")

    val isFirstTimeTildeltNavKontor = historikk.none { it.type == HistorikkType.TILDELT_NAV_KONTOR }
    // Ikke si at søknaden er videresendt hvis søknaden er en papirsøknad (originalSoknadNAV == null)
    // og det er første gang den er tildelt et nav-kontor
    val hendelseTekstType =
        if (isPapirSoknad && isFirstTimeTildeltNavKontor) {
            if (destinasjon != null) {
                HendelseTekstType.SOKNAD_VIDERESENDT_PAPIRSOKNAD_MED_NORG_ENHET
            } else {
                HendelseTekstType.SOKNAD_VIDERESENDT_PAPIRSOKNAD_UTEN_NORG_ENHET
            }
        } else {
            if (destinasjon != null) {
                HendelseTekstType.SOKNAD_VIDERESENDT_MED_NORG_ENHET
            } else {
                HendelseTekstType.SOKNAD_VIDERESENDT_UTEN_NORG_ENHET
            }
        }

    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Tildelt Navkontor. Beskrivelse: ${hendelseTekstType.name}")
    historikk.add(
        Hendelse(
            hendelseTekstType,
            hendelse.hendelsestidspunkt.toLocalDateTime(),
            type = HistorikkType.TILDELT_NAV_KONTOR,
            tekstArgument = destinasjon,
        ),
    )
}
