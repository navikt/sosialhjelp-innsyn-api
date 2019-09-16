package no.nav.sbl.sosialhjelpinnsynapi.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg/send", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(@PathVariable fiksDigisosId: String, @RequestParam("data") files: MutableList<MultipartFile>,
                @RequestParam("metadata") metadataMultipartFile: MultipartFile,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<VedleggOpplastingResponse>> {
        val mapper = jacksonObjectMapper()
        val metadata: MutableList<OpplastetVedleggMetadata> = mapper.readValue(metadataMultipartFile.bytes)

        val originalFileList = files.toList()

        files.forEach { file -> if (file.size > MAKS_TOTAL_FILSTORRELSE) {
            metadata.forEach { it.filer.removeIf { it.filnavn == file.originalFilename } }
        }
        }
        metadata.removeIf { it.filer.isEmpty() }
        files.removeIf { it.size > MAKS_TOTAL_FILSTORRELSE }

        if (files.isEmpty() || metadata.size != files.size) {
            return ResponseEntity.ok(emptyList())
        }

        vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId, files, metadata, token)

        return ResponseEntity.ok(originalFileList.map {
            if (files.contains(it)) VedleggOpplastingResponse(it.originalFilename, it.size) else VedleggOpplastingResponse(it.originalFilename, -1)
        })
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

data class OpplastetVedleggMetadata (
        val type: String,
        val tilleggsinfo: String,
        val filer: MutableList<sendtFil>
)

data class sendtFil (
        val filnavn: String
)