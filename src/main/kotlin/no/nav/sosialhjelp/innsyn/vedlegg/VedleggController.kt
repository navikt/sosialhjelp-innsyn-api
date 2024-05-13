package no.nav.sosialhjelp.innsyn.vedlegg

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.annotation.MultipartConfig
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sosialhjelp.filkonvertering.service.FileConversionService
import no.nav.sosialhjelp.filkonvertering.service.VedleggKonverteringOpplasting
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.hendelser.RequestAttributesContext
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_IDPORTEN_LOA_HIGH
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.ACR_LEVEL4
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.dto.OppgaveOpplastingResponse
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggOpplastingResponse
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.util.MultiValueMap
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDate
import java.util.UUID

@ProtectedWithClaims(issuer = SELVBETJENING, claimMap = [ACR_LEVEL4, ACR_IDPORTEN_LOA_HIGH], combineWithOr = true)
@MultipartConfig(location = "/tmp", maxFileSize = 10 * 1024 * 1024, maxRequestSize = 150 * 1024 * 1024, fileSizeThreshold = 5 * 1024 * 1024)
@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(
    private val vedleggOpplastingService: VedleggOpplastingService,
    private val fileConverterService: FileConversionService,
    private val vedleggService: VedleggService,
    private val clientProperties: ClientProperties,
    private val tilgangskontroll: TilgangskontrollService,
    private val xsrfGenerator: XsrfGenerator,
    private val eventService: EventService,
    private val fiksClient: FiksClient,
) {
    @PostMapping("/{fiksDigisosId}/vedlegg/konverter", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun konverterVedlegg(
        @PathVariable fiksDigisosId: String,
        @RequestPart("files") rawFiles: List<MultipartFile>,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
        request: HttpServletRequest,
    ): ResponseEntity<MultiValueMap<String, HttpEntity<*>>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                log.info("Forsøker å konvertere vedlegg")
                tilgangskontroll.sjekkTilgang(token)
                xsrfGenerator.sjekkXsrfToken(request)

                check(rawFiles.isNotEmpty()) { "Ingen filer sendt til konvertering" }

                val opplastinger =
                    rawFiles.map {
                        VedleggKonverteringOpplasting(it)
                    }.let {
                        fileConverterService.convertFileToPdf(it)
                    }
                val bodyBuilder = MultipartBodyBuilder()
                opplastinger.onEach { bodyBuilder.part(it.key.convertedName, HttpEntity(it.value.getOrThrow()), MediaType.APPLICATION_PDF) }
                println("Har lagt til parts")
                val body = bodyBuilder.build()
                println("Har bygget body: ${body}")

                ResponseEntity.ok().contentType(MediaType.MULTIPART_FORM_DATA).body(body)
            }
        }

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendVedlegg(
        @PathVariable fiksDigisosId: String,
        @RequestPart("files") rawFiles: List<MultipartFile>,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
        request: HttpServletRequest,
    ): ResponseEntity<List<OppgaveOpplastingResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                log.info("Forsøker å starter ettersendelse")
                tilgangskontroll.sjekkTilgang(token)
                xsrfGenerator.sjekkXsrfToken(request)

                val (metadata, files) = getMetadataAndRemoveFromFileList(rawFiles)

                check(files.isNotEmpty()) { "Ingen filer i forsendelse" }

                metadata.flatMap { it.filer }.onEach { fil ->
                    fil.fil = files.find {
                        it.originalFilename?.contains(fil.uuid.toString()) ?: false
                    } ?: error("Fil i metadata var ikke i listen over filer")
                }

                val oppgaveValideringList =
                    vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId, metadata, token)
                ResponseEntity.ok(mapToResponse(oppgaveValideringList))
            }
        }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    fun hentVedlegg(
        @PathVariable fiksDigisosId: String,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) token: String,
    ): ResponseEntity<List<VedleggResponse>> =
        runBlocking {
            withContext(MDCContext() + RequestAttributesContext()) {
                tilgangskontroll.sjekkTilgang(token)
                val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, token, true)
                val model = eventService.createModel(digisosSak, token)

                val internalVedleggList: List<InternalVedlegg> = vedleggService.hentAlleOpplastedeVedlegg(digisosSak, model, token)
                if (internalVedleggList.isEmpty()) {
                    return@withContext ResponseEntity(HttpStatus.NO_CONTENT)
                }
                // mapper til en flat liste av VedleggResponse
                val vedleggResponses =
                    internalVedleggList
                        .flatMap {
                            it.dokumentInfoList.map { dokumentInfo ->
                                VedleggResponse(
                                    removeUUIDFromFilename(dokumentInfo.filnavn),
                                    dokumentInfo.storrelse,
                                    hentDokumentlagerUrl(clientProperties, dokumentInfo.dokumentlagerDokumentId),
                                    it.type,
                                    it.tilleggsinfo,
                                    it.tidspunktLastetOpp,
                                )
                            }
                        }
                ResponseEntity.ok(vedleggResponses.distinct())
            }
        }

    private fun mapToResponse(oppgaveValideringList: List<OppgaveValidering>) =
        oppgaveValideringList.map {
            OppgaveOpplastingResponse(
                it.type,
                it.tilleggsinfo,
                it.innsendelsesfrist,
                it.hendelsetype,
                it.hendelsereferanse,
                it.filer.map { fil -> VedleggOpplastingResponse(fil.filename, fil.status.result) },
            )
        }

    private fun getMetadataAndRemoveFromFileList(files: List<MultipartFile>): Pair<List<OpplastetVedleggMetadata>, List<MultipartFile>> {
        val metadataJson =
            files.firstOrNull { it.originalFilename == "metadata.json" }
                ?: throw IllegalStateException("Mangler metadata.json. Totalt antall filer var ${files.size}")
        return Pair(objectMapper.readValue<List<OpplastetVedleggMetadata>>(metadataJson.bytes), files - metadataJson)
    }

    fun removeUUIDFromFilename(filename: String): String {
        val indexOfFileExtension = filename.lastIndexOf(".")
        if (indexOfFileExtension != -1 && indexOfFileExtension > LENGTH_OF_UUID_PART &&
            filename.substring(indexOfFileExtension - LENGTH_OF_UUID_PART).startsWith("-")
        ) {
            val extension = filename.substring(indexOfFileExtension, filename.length)
            return filename.substring(0, indexOfFileExtension - LENGTH_OF_UUID_PART) + extension
        }
        return filename
    }

    companion object {
        private val log by logger()

        private const val LENGTH_OF_UUID_PART = 9
    }
}

data class OpplastetVedleggMetadata(
    val type: String,
    val tilleggsinfo: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: MutableList<OpplastetFil>,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
)

data class OpplastetFil(
    var filnavn: String,
    val uuid: UUID,
) {
    lateinit var fil: MultipartFile
    lateinit var validering: FilValidering
}
