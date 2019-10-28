package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonVedtakFattet, clientProperties: ClientProperties) {

    val utfallString = hendelse.utfall?.name
    val utfall = if (utfallString == null) null else UtfallVedtak.valueOf(utfallString)
    val vedtaksfilUrl = hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse)

    val vedtakFattet = Vedtak(utfall, vedtaksfilUrl, toLocalDateTime(hendelse.hendelsestidspunkt).toLocalDate())

    val sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse }
    if (sakForReferanse != null) {
        sakForReferanse.vedtak.add(vedtakFattet)
    } else {
        val sak = Sak(
                hendelse.saksreferanse,
                null,
                null,
                mutableListOf(vedtakFattet),
                mutableListOf(),
                mutableListOf()
        )
        saker.add(sak)
    }

    val sak = saker.first { it.referanse == hendelse.saksreferanse }
    val beskrivelse = "${sak.tittel ?: DEFAULT_TITTEL} er ferdig behandlet"

    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt), vedtaksfilUrl))
}