package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sosialhjelp.innsyn.common.VIS_BREVET
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.utils.hentUrlFraFilreferanse
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonForelopigSvar, clientProperties: ClientProperties) {

    forelopigSvar = ForelopigSvar(true, hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse))

    val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"
    historikk.add(
        Hendelse(
            beskrivelse,
            hendelse.hendelsestidspunkt.toLocalDateTime(),
            UrlResponse(
                VIS_BREVET,
                hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)
            )
        )
    )
}
