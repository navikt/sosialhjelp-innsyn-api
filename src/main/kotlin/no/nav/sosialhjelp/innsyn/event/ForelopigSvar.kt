package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonForelopigSvar
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.utils.hentUrlFraFilreferanse
import no.nav.sosialhjelp.innsyn.utils.toLocalDateTime
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JsonForelopigSvar::class.java.name)

fun InternalDigisosSoker.apply(hendelse: JsonForelopigSvar, clientProperties: ClientProperties) {

    forelopigSvar = ForelopigSvar(true, hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse))

    log.info("Hendelse: Tidspunkt: ${hendelse.hendelsestidspunkt} Forelopig svar. Du har fått et brev om saksbehandlingstiden for søknaden din.")
    historikk.add(
        Hendelse(
            HendelseTekstType.BREV_OM_SAKSBEANDLINGSTID,
            hendelse.hendelsestidspunkt.toLocalDateTime(),
            UrlResponse(
                VIS_BREVET,
                hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse)
            )
        )
    )
}
