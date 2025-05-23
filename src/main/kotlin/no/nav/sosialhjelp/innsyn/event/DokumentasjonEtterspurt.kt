package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonDokumentasjonEtterspurt
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.utils.hentUrlFraFilreferanse
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonDokumentasjonEtterspurt::class.java.name)

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
                )
            }.toMutableList()

    if (status == SoknadsStatus.FERDIGBEHANDLET) {
        log.warn("Dokumentasjon etterspurt etter at søknad er satt til ferdigbehandlet. fiksDigisosId: $fiksDigisosId")
    }
    if (hendelse.dokumenter.isNotEmpty() && hendelse.forvaltningsbrev != null) {
        val url = hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)
        log.info("Hendelse: Dokumentasjon etterspurt. Vi trenger flere opplysninger til søknaden din.")
        historikk.add(
            Hendelse(
                HendelseTekstType.ETTERSPOR_MER_DOKUMENTASJON,
                hendelse.hendelsestidspunkt.toLocalDateTime(),
                UrlResponse(HendelseTekstType.VIS_BREVET_LENKETEKST, url),
            ),
        )
    }

    if (prevSize > 0 && oppgaver.size == 0 && status != SoknadsStatus.FERDIGBEHANDLET && status != SoknadsStatus.BEHANDLES_IKKE) {
        log.info(
            "Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Dokumentasjon etterspurt. " +
                "Vi har sett på opplysningene dine og vil gi beskjed om vi trenger noe mer fra deg.",
        )
        historikk.add(Hendelse(HendelseTekstType.ETTERSPOR_IKKE_MER_DOKUMENTASJON, hendelse.hendelsestidspunkt.toLocalDateTime(), null))
    }
}
