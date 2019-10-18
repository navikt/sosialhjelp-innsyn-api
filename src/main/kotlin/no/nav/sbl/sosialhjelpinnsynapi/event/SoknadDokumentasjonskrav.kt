package no.nav.sbl.sosialhjelpinnsynapi.event

import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.Oppgave
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.unixToLocalDateTime
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VEDLEGG_KREVES_STATUS
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService

fun InternalDigisosSoker.applySoknadKrav(fiksDigisosId: String, originalSoknadNAV: OriginalSoknadNAV, vedleggService: VedleggService, timestampSendt: Long, token: String) {
    val vedleggKreves = vedleggService.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, fiksDigisosId, originalSoknadNAV, token)

    oppgaver = vedleggKreves
            .filterNot { it.type == "annet" && it.tilleggsinfo == "annet" }
            .map { Oppgave(it.type, it.tilleggsinfo, null, unixToLocalDateTime(timestampSendt), false) }
            .toMutableList()
    }