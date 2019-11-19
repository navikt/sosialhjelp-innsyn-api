package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.ForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonForelopigSvar, clientProperties: ClientProperties) {

    forelopigSvar = ForelopigSvar(true, hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse))

    val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"
    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt), hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)))
}