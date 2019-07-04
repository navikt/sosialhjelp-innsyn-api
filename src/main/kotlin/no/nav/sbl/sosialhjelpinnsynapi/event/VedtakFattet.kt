package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse

fun InternalDigisosSoker.applyVedtakFattet(hendelse: JsonVedtakFattet, clientProperties: ClientProperties) {

    // TODO: håndter historikk

    val filtered = saker.filter { it.referanse == hendelse.referanse }
    if (filtered.isNotEmpty() && filtered.size == 1) {

        val sak = filtered[0]
        sak.vedtak.add(Vedtak(
                UtfallVedtak.valueOf(hendelse.utfall.utfall.name),
                hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse)
        ))
    } else {
        // TODO: hva hvis vedtakFattet mottas _før_ saksStatus ?
    }


}