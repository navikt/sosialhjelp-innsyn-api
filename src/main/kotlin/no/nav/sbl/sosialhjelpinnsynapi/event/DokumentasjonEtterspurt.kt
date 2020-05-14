package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.sosialhjelpinnsynapi.common.VIS_BREVET
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.UrlResponse
import no.nav.sbl.sosialhjelpinnsynapi.utils.hentUrlFraFilreferanse
import no.nav.sbl.sosialhjelpinnsynapi.utils.sha256
import no.nav.sbl.sosialhjelpinnsynapi.utils.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonEtterspurt, clientProperties: ClientProperties) {
    val prevSize = oppgaver.size

    oppgaver = hendelse.dokumenter
            .map { Oppgave(sha256(it.innsendelsesfrist), it.dokumenttype, it.tilleggsinformasjon, it.innsendelsesfrist.toLocalDateTime(), hendelse.hendelsestidspunkt.toLocalDateTime(), true) }
            .toMutableList()

    if (hendelse.dokumenter.isNotEmpty() && hendelse.forvaltningsbrev != null) {
        val beskrivelse = "Du må sende dokumentasjon"
        val url = hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)
        historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), UrlResponse(VIS_BREVET, url)))
    }

    if (prevSize > 0 && oppgaver.size == 0 && status != SoknadsStatus.FERDIGBEHANDLET && status != SoknadsStatus.BEHANDLES_IKKE) {
        val beskrivelse = "Vi har sett på dokumentene dine og vil gi beskjed om vi trenger mer fra deg."
        historikk.add(Hendelse(beskrivelse, hendelse.hendelsestidspunkt.toLocalDateTime(), null))
    }
}
