package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService

suspend fun InternalDigisosSoker.applySoknadKrav(
    digisosSak: DigisosSak,
    vedleggService: VedleggService,
    timestampSendt: Long,
    token: Token,
) {
    val vedleggKreves = vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, digisosSak, token)

    oppgaver =
        vedleggKreves
            .filterNot { it.type == "annet" && it.tilleggsinfo == "annet" }
            .map {
                Oppgave(
                    sha256(timestampSendt.toString()),
                    it.type,
                    it.tilleggsinfo,
                    JsonVedlegg.HendelseType.SOKNAD,
                    it.hendelseReferanse,
                    null,
                    unixToLocalDateTime(timestampSendt),
                    false,
                )
            }.toMutableList()
}
