package no.nav.sosialhjelp.innsyn.event

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.service.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.sha256
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime

fun InternalDigisosSoker.applySoknadKrav(fiksDigisosId: String, originalSoknadNAV: OriginalSoknadNAV, vedleggService: VedleggService, timestampSendt: Long, token: String) {
    val vedleggKreves = vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, fiksDigisosId, originalSoknadNAV, token)

    oppgaver = vedleggKreves
            .filterNot { it.type == "annet" && it.tilleggsinfo == "annet" }
            .map { Oppgave(sha256(timestampSendt.toString()),
                    it.type,
                    it.tilleggsinfo,
                    JsonVedlegg.HendelseType.SOKNAD,
                    it.hendelseReferanse,
                    null,
                    unixToLocalDateTime(timestampSendt),
                    false) }
            .toMutableList()
}