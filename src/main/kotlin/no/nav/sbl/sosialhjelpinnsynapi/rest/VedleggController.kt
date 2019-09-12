package no.nav.sbl.sosialhjelpinnsynapi.rest

import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggOpplastingService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Unprotected
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(private val vedleggOpplastingService: VedleggOpplastingService,
                        private val vedleggService: VedleggService,
                        private val clientProperties: ClientProperties) {

    val MAKS_TOTAL_FILSTORRELSE: Int = 1024 * 1024 * 10

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
    @PostMapping("/{fiksDigisosId}/vedlegg/sendDem")
    fun sendVedleggTilFiks(@PathVariable fiksDigisosId: String): ResponseEntity<String> {
        val response = vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId)

        return ResponseEntity.ok(response)
    }

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg/send", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendDem(@PathVariable fiksDigisosId: String, @RequestParam("files") files: MutableList<MultipartFile>,
                @RequestParam("metadata") metadata: MutableList<JsonVedlegg>): ResponseEntity<List<VedleggOpplastingResponse>> {

        files.forEach { file -> if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            metadata.forEach { it.filer.removeIf { it.filnavn == file.originalFilename } }
        }
        }
        metadata.removeIf { it.filer.isEmpty() }
        files.removeIf { it.size > MAKS_TOTAL_FILSTORRELSE }

        if (files.isEmpty() || metadata.size != files.size) {
            return ResponseEntity.ok(emptyList())
        }

        vedleggOpplastingService.sendVedleggTilFiks2(fiksDigisosId, files, metadata)

        return ResponseEntity.ok(files.map { VedleggOpplastingResponse(it.originalFilename, it.size) })
    }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun hentVedlegg(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<VedleggResponse>> {
        val internalVedleggList: List<InternalVedlegg> = vedleggService.hentAlleVedlegg(fiksDigisosId)
        if (internalVedleggList.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
        }
        // mapper til en flat liste av VedleggResponse
        val vedleggResponses = internalVedleggList
                .flatMap {
                    it.dokumentInfoList.map { dokumentInfo ->
                        VedleggResponse(
                                dokumentInfo.filnavn,
                                dokumentInfo.storrelse,
                                hentDokumentlagerUrl(clientProperties, dokumentInfo.dokumentlagerDokumentId),
                                it.type,
                                it.tidspunktLastetOpp)
                    }
                }
        return ResponseEntity.ok(vedleggResponses.distinct())
    }
}