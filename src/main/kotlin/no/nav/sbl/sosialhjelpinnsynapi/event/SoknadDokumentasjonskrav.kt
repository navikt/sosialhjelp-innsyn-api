package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.utils.sha256
import no.nav.sbl.sosialhjelpinnsynapi.utils.unixToLocalDateTime
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV

fun InternalDigisosSoker.applySoknadKrav(fiksDigisosId: String, originalSoknadNAV: OriginalSoknadNAV, vedleggService: VedleggService, timestampSendt: Long, token: String) {
    val vedleggKreves = vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, fiksDigisosId, originalSoknadNAV, token)

    oppgaver = vedleggKreves
            .filterNot { it.type == "annet" && it.tilleggsinfo == "annet" }
            .map { Oppgave(sha256(timestampSendt.toString()), it.type, it.tilleggsinfo, null, unixToLocalDateTime(timestampSendt), false) }
            .toMutableList()
}