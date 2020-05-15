package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.common.VIS_BREVET
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UrlResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.UtfallVedtak
import no.nav.sbl.sosialhjelpinnsynapi.domain.Vedtak
import no.nav.sbl.sosialhjelpinnsynapi.service.saksstatus.DEFAULT_TITTEL
import no.nav.sbl.sosialhjelpinnsynapi.utils.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.utils.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonVedtakFattet, clientProperties: ClientProperties) {

    val utfallString = hendelse.utfall?.name
    val utfall = if (utfallString == null) null else UtfallVedtak.valueOf(utfallString)
    val vedtaksfilUrl = hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse)

    val vedtakFattet = Vedtak(utfall, vedtaksfilUrl, hendelse.hendelsestidspunkt.toLocalDateTime().toLocalDate())

    var sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse || it.referanse == "default" }

    if (sakForReferanse == null) {
        // Opprett ny Sak
        sakForReferanse = Sak(
                referanse = hendelse.saksreferanse ?: "default",
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = DEFAULT_TITTEL,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf()
        )
        saker.add(sakForReferanse)
    }
    sakForReferanse.vedtak.add(vedtakFattet)

    val beskrivelse = "${sakForReferanse.tittel ?: DEFAULT_TITTEL} er ferdig behandlet"

    historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), UrlResponse(VIS_BREVET, vedtaksfilUrl)))
}