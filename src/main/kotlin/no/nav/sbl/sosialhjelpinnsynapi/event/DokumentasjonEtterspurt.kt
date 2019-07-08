package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.applyDokumentasjonEtterspurt(hendelse: JsonDokumentasjonEtterspurt, clientProperties: ClientProperties) {

//  TODO: Når DB er på plass må vi sjekke om denne dokumentasjonenEtterspurt har blitt oppfylt av søker

    oppgaver.addAll(hendelse.dokumenter.map { Oppgave(it.dokumenttype, it.tilleggsinformasjon, toLocalDateTime(it.innsendelsesfrist)) })

    val beskrivelse = "Du må laste opp mer dokumentasjon"
    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt), hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)))
}