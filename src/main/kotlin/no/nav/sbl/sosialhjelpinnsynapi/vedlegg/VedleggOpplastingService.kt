package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse

interface VedleggOpplastingService {
    fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse>

    fun sendVedleggTilFiks(fiksDigisosId: String): String
}