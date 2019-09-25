package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonEtterspurt, clientProperties: ClientProperties) {
    oppgaver = hendelse.dokumenter
            .map { Oppgave(it.dokumenttype, it.tilleggsinformasjon, toLocalDateTime(it.innsendelsesfrist), toLocalDateTime(hendelse.hendelsestidspunkt)) }
            .toMutableList()

    val beskrivelse = if (hendelse.dokumenter.isEmpty()) "Veileder har lest dokumentene du har sendt" else "Veileder har oppdatert dine dokumentasjonskrav"
    val url = if (hendelse.dokumenter.isEmpty()) null else hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)
    historikk.add(Hendelse(beskrivelse, toLocalDateTime(hendelse.hendelsestidspunkt), url))
}