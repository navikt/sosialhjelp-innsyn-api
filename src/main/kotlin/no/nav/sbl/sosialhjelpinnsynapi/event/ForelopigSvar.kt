package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.common.VIS_BREVET
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.UrlResponse
import no.nav.sbl.sosialhjelpinnsynapi.utils.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.utils.toLocalDateTime

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