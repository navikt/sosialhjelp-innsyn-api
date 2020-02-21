package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.common.VIS_BREVET
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonVedtakFattet, clientProperties: ClientProperties) {

    val utfallString = hendelse.utfall?.name
    val utfall = if (utfallString == null) null else UtfallVedtak.valueOf(utfallString)
    val vedtaksfilUrl = hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse)

    val vedtakFattet = Vedtak(utfall, vedtaksfilUrl, hendelse.hendelsestidspunkt.toLocalDateTime().toLocalDate())

    var sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse || it.referanse == "default" }

    if (sakForReferanse == null) {
        // Opprett ny Sak
        sakForReferanse = Sak(
                hendelse.saksreferanse ?: "default",
                SaksStatus.UNDER_BEHANDLING,
                DEFAULT_TITTEL,
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
        )
        saker.add(sakForReferanse)
    }
    sakForReferanse.vedtak.add(vedtakFattet)

    val beskrivelse = "${sakForReferanse.tittel ?: DEFAULT_TITTEL} er ferdig behandlet"

    historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), UrlResponse(VIS_BREVET, vedtaksfilUrl)))
}