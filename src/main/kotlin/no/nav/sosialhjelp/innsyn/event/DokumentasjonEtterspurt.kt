package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Forvaltningsbrev
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.utils.hentUrlFraFilreferanse
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime

fun InternalDigisosSoker.apply(
    hendelse: JsonDokumentasjonEtterspurt,
    clientProperties: ClientProperties,
) {
    val prevSize = oppgaver.size

    oppgaver =
        hendelse.dokumenter
            .map {
                Oppgave(
                    sha256(it.innsendelsesfrist),
                    it.dokumenttype,
                    it.tilleggsinformasjon,
                    JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT,
                    it.dokumentreferanse,
                    it.innsendelsesfrist.toLocalDateTime(),
                    hendelse.hendelsestidspunkt.toLocalDateTime(),
                    true,
                    forvaltningsbrev =
                        hendelse.forvaltningsbrev?.let { forvaltningsbrev ->
                            Forvaltningsbrev(
                                hentUrlFraFilreferanse(clientProperties, forvaltningsbrev.referanse),
                                hendelse.hendelsestidspunkt.toLocalDateTime(),
                            )
                        },
                )
            }.toMutableList()

    if (hendelse.dokumenter.isNotEmpty()) {
        hendelse.forvaltningsbrev?.let {
            val url = hentUrlFraFilreferanse(clientProperties, it.referanse)
            historikk.add(
                Hendelse(
                    HendelseTekstType.ETTERSPOR_MER_DOKUMENTASJON,
                    hendelse.hendelsestidspunkt.toLocalDateTime(),
                    UrlResponse(HendelseTekstType.VIS_BREVET_LENKETEKST, url),
                ),
            )
        }
    }

    if (prevSize > 0 && oppgaver.isEmpty() && status != SoknadsStatus.BEHANDLES_IKKE) {
        historikk.add(Hendelse(HendelseTekstType.ETTERSPOR_IKKE_MER_DOKUMENTASJON, hendelse.hendelsestidspunkt.toLocalDateTime(), null))
    }
}
