package no.nav.sbl.sosialhjelpinnsynapi.vedleggopplasting

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse

interface VedleggOpplastingService {
    fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse>

    fun lastOppVedleggTilFiks(fiksDigisosId: String): String
}