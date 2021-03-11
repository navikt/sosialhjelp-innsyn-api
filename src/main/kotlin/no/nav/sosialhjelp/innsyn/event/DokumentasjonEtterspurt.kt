package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.common.VIS_BREVET
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.utils.hentUrlFraFilreferanse
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime

fun InternalDigisosSoker.apply(hendelse: JsonDokumentasjonEtterspurt, clientProperties: ClientProperties) {
    val prevSize = oppgaver.size

    oppgaver = hendelse.dokumenter
            .map { Oppgave(sha256(it.innsendelsesfrist),
                    it.dokumenttype,
                    it.tilleggsinformasjon,
                    JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT,
                    it.dokumentreferanse,
                    it.innsendelsesfrist.toLocalDateTime(),
                    hendelse.hendelsestidspunkt.toLocalDateTime(),
                    true) }
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
