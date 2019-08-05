package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.vedleggopplasting.VedleggOpplastingService
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(private val vedleggOpplastingService: VedleggOpplastingService) {

    // Last opp vedlegg for mellomlagring
    @PostMapping("/{fiksDigisosId}/vedlegg/lastOpp", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun lastOppVedlegg(@PathVariable fiksDigisosId: String, @RequestParam("file") files: List<MultipartFile>): ResponseEntity<List<VedleggOpplastingResponse>> {

        // Sjekk om fileSize overskrider MAKS_FILSTORRELSE

        files.forEach { println("file name: ${it.originalFilename}") }

        val bytes = files[0].bytes
        val inputStream = files[0].inputStream

        // hva bør input være? inputStream / bytes / files ?
        val response = vedleggOpplastingService.mellomlagreVedlegg(fiksDigisosId, files)

        return ResponseEntity.ok(response)
    }

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg/send")
    fun sendVedleggTilFiks(@PathVariable fiksDigisosId: String): ResponseEntity<String> {
        val response = vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId)

        return ResponseEntity.ok(response)
    }
}