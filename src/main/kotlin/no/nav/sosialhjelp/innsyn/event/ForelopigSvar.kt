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

fun InternalDigisosSoker.apply(
    hendelse: JsonForelopigSvar,
    clientProperties: ClientProperties,
) {
    forelopigSvar =
        ForelopigSvar(
            true,
            hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse),
            hendelse.hendelsestidspunkt.toLocalDateTime(),
        )

    historikk.add(
        Hendelse(
            HendelseTekstType.BREV_OM_SAKSBEANDLINGSTID,
            hendelse.hendelsestidspunkt.toLocalDateTime(),
            UrlResponse(
                HendelseTekstType.VIS_BREVET_LENKETEKST,
                hentUrlFraFilreferanse(clientProperties, hendelse.forvaltningsbrev.referanse),
            ),
        ),
    )
}
