package no.nav.sosialhjelp.innsyn.rest

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.config.XsrfGenerator.sjekkXsrfToken
import no.nav.sosialhjelp.innsyn.domain.OppgaveOpplastingResponse
import no.nav.sosialhjelp.innsyn.domain.VedleggOpplastingResponse
import no.nav.sosialhjelp.innsyn.domain.VedleggResponse
import no.nav.sosialhjelp.innsyn.service.tilgangskontroll.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.OppgaveValidering
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggOpplastingService
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import javax.servlet.http.HttpServletRequest

const val LENGTH_OF_UUID_PART = 9

@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(
        private val vedleggOpplastingService: VedleggOpplastingService,
        private val vedleggService: VedleggService,
        private val clientProperties: ClientProperties,
        private val tilgangskontrollService: TilgangskontrollService
) {

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(@PathVariable fiksDigisosId: String,
                    @RequestParam("files") files: MutableList<MultipartFile>,
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
                    request: HttpServletRequest
    ): ResponseEntity<List<OppgaveOpplastingResponse>> {
        log.info("Forsøker å starter ettersendelse")
        tilgangskontrollService.sjekkTilgang()
        sjekkXsrfToken(fiksDigisosId, request)

        val metadata: MutableList<OpplastetVedleggMetadata> = getMetadataAndRemoveFromFileList(files)
        validateFileListNotEmpty(files)

        val oppgaveValideringList = vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId, files, metadata, token)
        return ResponseEntity.ok(mapToResponse(oppgaveValideringList))
    }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    fun hentVedlegg(@PathVariable fiksDigisosId: String, @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<List<VedleggResponse>> {
        tilgangskontrollService.sjekkTilgang()

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

    private fun mapToResponse(oppgaveValideringList: List<OppgaveValidering>) =
            oppgaveValideringList.map {
                OppgaveOpplastingResponse(
                        it.type,
                        it.tilleggsinfo,
                        it.innsendelsesfrist,
                        it.filer.map { VedleggOpplastingResponse(it.filename, it.status.result.name) }
                )
            }

    private fun validateFileListNotEmpty(files: MutableList<MultipartFile>) {
        if (files.isEmpty()) {
            throw IllegalStateException("Ingen filer i forsendelse")
        }
    }

    private fun getMetadataAndRemoveFromFileList(files: MutableList<MultipartFile>): MutableList<OpplastetVedleggMetadata> {
        val metadataJson = files.firstOrNull { it.originalFilename == "metadata.json" }
                ?: throw IllegalStateException("Mangler metadata.json. Totalt antall filer var ${files.size}")
        files.removeIf { it.originalFilename == "metadata.json" }
        return objectMapper.readValue(metadataJson.bytes)
    }

    fun removeUUIDFromFilename(filename: String): String {
        val indexOfFileExtention = filename.lastIndexOf(".")
        if (indexOfFileExtention != -1 && indexOfFileExtention > LENGTH_OF_UUID_PART &&
            filename.substring(indexOfFileExtention - LENGTH_OF_UUID_PART).startsWith("-")
        ) {
            val extention = filename.substring(indexOfFileExtention, filename.length)
            return filename.substring(0, indexOfFileExtention - LENGTH_OF_UUID_PART) + extention
        }
        return filename
    }

    companion object {
        private val log by logger()
    }
}

data class OpplastetVedleggMetadata(
        val type: String,
        val tilleggsinfo: String?,
        val filer: MutableList<OpplastetFil>,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val innsendelsesfrist: LocalDate?
)

data class OpplastetFil(
        var filnavn: String
)
