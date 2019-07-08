package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.applyForelopigSvar(hendelse: JsonForelopigSvar, clientProperties: ClientProperties) {

    val beskrivelse = "Du har fått et brev om saksbehandlingstiden for søknaden din"

    // TODO:
    //  skal frontend viser varsel når dette er nyeste hendelse?
    //  legge til noe i modellen?

    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt), hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)))
}