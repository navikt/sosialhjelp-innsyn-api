package no.nav.sbl.sosialhjelpinnsynapi.rest

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
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

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg/send", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(@PathVariable fiksDigisosId: String,
                    @RequestParam("files") files: MutableList<MultipartFile>,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<VedleggOpplastingResponse>> {
        val metadataJson = files.firstOrNull { it.originalFilename == "metadata.json" }
                ?: throw IllegalStateException("Mangler metadata.json")
        val metadata: MutableList<OpplastetVedleggMetadata> = objectMapper.readValue(metadataJson.bytes)
        files.removeIf { it.originalFilename == "metadata.json" }

        val vedleggOpplastingResponseList = vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId, files, metadata, token)
        return ResponseEntity.ok(vedleggOpplastingResponseList)
    }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentVedlegg(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<VedleggResponse>> {
        val internalVedleggList: List<InternalVedlegg> = vedleggService.hentAlleOpplastedeVedlegg(fiksDigisosId, token)
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
                                it.tilleggsinfo,
                                it.tidspunktLastetOpp)
                    }
                }
        return ResponseEntity.ok(vedleggResponses.distinct())
    }
}

data class OpplastetVedleggMetadata (
        val type: String,
        val tilleggsinfo: String?,
        val filer: MutableList<OpplastetFil>
)

data class OpplastetFil (
        val filnavn: String
)