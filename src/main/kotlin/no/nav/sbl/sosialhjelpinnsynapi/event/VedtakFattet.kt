package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.enumNameToLowercase
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.applyVedtakFattet(hendelse: JsonVedtakFattet, clientProperties: ClientProperties) {

    val utfall = UtfallVedtak.valueOf(hendelse.utfall.utfall.name)
    val vedtaksfilUrl = hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse)

    val vedtakFattet = Vedtak(utfall, vedtaksfilUrl)

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.referanse }
    if (sakForReferanse != null) {
        sakForReferanse.vedtak.add(vedtakFattet)
    } else {
        // Ny Sak opprettes med default-verdier
        val sak = Sak(
                hendelse.referanse,
                SaksStatus.UNDER_BEHANDLING, //TODO: midlertidig SaksStatus for disse tilfellene?
                DEFAULT_TITTEL,
                mutableListOf(vedtakFattet),
                mutableListOf())
        saker.add(sak)
    }

    val sak = saker.first { it.referanse == hendelse.referanse }
    val beskrivelse = if (hendelse.utfall == null) { // Hvis utfall kan være null
        "Vedtak fattet"
    } else {
        "${sak.tittel} er ${enumNameToLowercase(utfall.name)}"
    }

    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt), vedtaksfilUrl))
}