package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse

interface VedleggService {
    fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse>

    fun lastOppVedleggTilFiks(fiksDigisosId: String): String
}