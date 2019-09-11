package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import org.springframework.web.multipart.MultipartFile

interface VedleggOpplastingService {
    fun mellomlagreVedlegg(fiksDigisosId: String, files: List<Any>): List<VedleggOpplastingResponse>

    fun sendVedleggTilFiks(fiksDigisosId: String): String

    fun sendVedleggTilFiks2(fiksDigisosId: String, files: List<MultipartFile>, metadata: List<JsonVedlegg>): String?
}