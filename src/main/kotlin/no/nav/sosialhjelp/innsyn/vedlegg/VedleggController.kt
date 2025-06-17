package no.nav.sosialhjelp.innsyn.vedlegg

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.dto.OppgaveOpplastingResponse
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggOpplastingResponse
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(
    private val vedleggOpplastingService: VedleggOpplastingService,
    private val vedleggService: VedleggService,
    private val clientProperties: ClientProperties,
    private val tilgangskontroll: TilgangskontrollService,
    private val eventService: EventService,
    private val fiksClient: FiksClient,
) {
    @PostMapping("/{fiksDigisosId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun sendVedlegg(
        @PathVariable fiksDigisosId: String,
        @RequestPart("files") rawFiles: Flux<FilePart>,
    ): List<OppgaveOpplastingResponse> {
        val allFiles = rawFiles.asFlow().toList()
        log.info("Forsøker å starte ettersendelse")
        tilgangskontroll.sjekkTilgang()

        // Ekstraherer metadata.json
        val metadata =
            allFiles
                .firstOrNull { it.filename() == "metadata.json" }
                ?.content()
                ?.let {
                    DataBufferUtils.join(it)
                }?.map {
                    val bytes = ByteArray(it.readableByteCount())
                    it.read(bytes)
                    DataBufferUtils.release(it)
                    objectMapper.readValue<List<OpplastetVedleggMetadata>>(bytes)
                }?.awaitSingleOrNull()
                ?.filter { it.filer.isNotEmpty() } ?: error("Missing metadata.json")

        val files =
            allFiles
                .filterNot { it.filename() == "metadata.json" }
                .also {
                    check(it.isNotEmpty()) { "Ingen filer i forsendelse" }
                    check(it.size <= 30) { "Over 30 filer i forsendelse: ${it.size} filer" }
                }

        val allDeclaredFilesHasAMatch =
            metadata.all { metadata ->
                metadata.filer.all { metadataFile ->
                    metadataFile.uuid.toString() in files.map { it.filename().substringBefore(".") }
                }
            }
        require(allDeclaredFilesHasAMatch) {
            "Ikke alle filer i metadata.json ble funnet i forsendelsen"
        }

        // Set hver fil på sitt tilhørende metadata-objekt
        files.onEach { file ->
            metadata
                .flatMap { it.filer }
                .find {
                    file.filename().contains(it.uuid.toString())
                }?.also {
                    it.fil = file
                }
        }

        return vedleggOpplastingService.processFileUpload(fiksDigisosId, metadata).mapToResponse()
    }

    @GetMapping("/{fiksDigisosId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    suspend fun hentVedlegg(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<VedleggResponse>> {
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()
        val digisosSak = fiksClient.hentDigisosSak(fiksDigisosId, TokenUtils.getToken())
        val model = eventService.createModel(digisosSak, token)

        val internalVedleggList: List<InternalVedlegg> = vedleggService.hentAlleOpplastedeVedlegg(digisosSak, model, token)
        if (internalVedleggList.isEmpty()) {
            return ResponseEntity(HttpStatus.NO_CONTENT)
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
        return ResponseEntity.ok(vedleggResponses.distinct())
    }

    fun removeUUIDFromFilename(filename: String): String {
        val indexOfFileExtension = filename.lastIndexOf(".")
        if (indexOfFileExtension != -1 &&
            indexOfFileExtension > LENGTH_OF_UUID_PART &&
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
    var filnavn: Filename,
    val uuid: UUID,
) {
    lateinit var fil: FilePart

    private var size: Long = -1

    suspend fun size(): Long {
        if (size == -1L) {
            size = fil.calculateContentLength()
        }
        return size
    }

    lateinit var validering: FilValidering
    lateinit var tikaMimeType: String
}

private fun List<OppgaveValidering>.mapToResponse() =
    map {
        OppgaveOpplastingResponse(
            it.type,
            it.tilleggsinfo,
            it.innsendelsesfrist,
            it.hendelsetype,
            it.hendelsereferanse,
            it.filer.map { fil -> VedleggOpplastingResponse(fil.filename, fil.status.result) },
        )
    }

suspend fun FilePart.calculateContentLength(): Long {
    val dataBuffer = DataBufferUtils.join(content()).awaitSingle()
    val contentLength = dataBuffer.readableByteCount().toLong()
    DataBufferUtils.release(dataBuffer)
    return contentLength
}
