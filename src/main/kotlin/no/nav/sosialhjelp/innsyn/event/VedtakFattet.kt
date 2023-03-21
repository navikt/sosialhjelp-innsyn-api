package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonVedtakFattet
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisossak.saksstatus.SaksStatusService.Companion.DEFAULT_SAK_TITTEL
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.domain.UtfallVedtak
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.utils.hentUrlFraFilreferanse
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonVedtakFattet::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonVedtakFattet, clientProperties: ClientProperties) {

    val utfallString = hendelse.utfall?.name
    val utfall = utfallString?.let { UtfallVedtak.valueOf(it) }
    val vedtaksfilUrl = hentUrlFraFilreferanse(clientProperties, hendelse.vedtaksfil.referanse)

    val vedtakFattet = Vedtak(utfall, vedtaksfilUrl, hendelse.hendelsestidspunkt.toLocalDateTime().toLocalDate())

    var sakForReferanse = saker.firstOrNull { it.referanse == hendelse.saksreferanse || it.referanse == "default" }

    if (sakForReferanse == null) {
        // Opprett ny Sak
        sakForReferanse = Sak(
            referanse = hendelse.saksreferanse ?: "default",
            saksStatus = SaksStatus.UNDER_BEHANDLING,
            tittel = DEFAULT_SAK_TITTEL,
            vedtak = mutableListOf(),
            utbetalinger = mutableListOf()
        )
        saker.add(sakForReferanse)
    }
    sakForReferanse.vedtak.add(vedtakFattet)

    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Vedtak fattet. <skjult tittel> er ferdigbehandlet")
    historikk.add(Hendelse(HendelseTekstType.SAK_FERDIGBEHANDLET, hendelse.hendelsestidspunkt.toLocalDateTime(), UrlResponse(VIS_BREVET, vedtaksfilUrl), tittelTekstArgument = sakForReferanse.tittel))
}
