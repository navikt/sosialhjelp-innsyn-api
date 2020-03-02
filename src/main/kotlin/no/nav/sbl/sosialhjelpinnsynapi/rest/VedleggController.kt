package no.nav.sbl.sosialhjelpinnsynapi.rest

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.config.XsrfGenerator.sjekkXsrfToken
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggOpplastingResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.hentDokumentlagerUrl
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggOpplastingService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest

const val LENGTH_OF_UUID_PART = 9

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(private val vedleggOpplastingService: VedleggOpplastingService,
                        private val vedleggService: VedleggService,
                        private val clientProperties: ClientProperties) {

    companion object {
        val log by logger()
    }
    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg/send", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(@PathVariable fiksDigisosId: String,
                    @RequestParam("files") files: MutableList<MultipartFile>,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
                    request: HttpServletRequest
    ): ResponseEntity<List<VedleggOpplastingResponse>> {
        sjekkXsrfToken(fiksDigisosId, request)

        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val allocatedMemory = runtime.totalMemory()

        val mb = 1024 * 1024

        log.info("maxMemory MB: ${maxMemory / mb}")
        log.info("allocated MB: ${allocatedMemory / mb}")
        log.info("free MB: ${runtime.freeMemory() / mb}")

        val metadataJson = files.firstOrNull { it.originalFilename == "metadata.json" }
                ?: throw IllegalStateException("Mangler metadata.json på digisosId=$fiksDigisosId")
        val metadata: MutableList<OpplastetVedleggMetadata> = objectMapper.readValue(metadataJson.bytes)
        files.removeIf { it.originalFilename == "metadata.json" }

        val vedleggOpplastingResponseList = vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId, files, metadata, token)
        return ResponseEntity.ok(vedleggOpplastingResponseList)
    }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = ["application/json;charset=UTF-8"])
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
                                removeUUIDFromFilename(dokumentInfo.filnavn),
                                dokumentInfo.storrelse,
                                hentDokumentlagerUrl(clientProperties, dokumentInfo.dokumentlagerDokumentId),
                                it.type,
                                it.tilleggsinfo,
                                it.tidspunktLastetOpp)
                    }
                }
        return ResponseEntity.ok(vedleggResponses.distinct())
    }

    fun removeUUIDFromFilename(filename: String): String {
        val indexOfFileExtention = filename.lastIndexOf(".")
        if (indexOfFileExtention != -1 && indexOfFileExtention > LENGTH_OF_UUID_PART) {
            if (filename.substring(indexOfFileExtention - LENGTH_OF_UUID_PART).startsWith("-")) {
                val extention = filename.substring(indexOfFileExtention, filename.length)
                return filename.substring(0, indexOfFileExtention - LENGTH_OF_UUID_PART) + extention
            }
        }
        return filename
    }
}

data class OpplastetVedleggMetadata (
        val type: String,
        val tilleggsinfo: String?,
        val filer: MutableList<OpplastetFil>
)

data class OpplastetFil (
        var filnavn: String
)
