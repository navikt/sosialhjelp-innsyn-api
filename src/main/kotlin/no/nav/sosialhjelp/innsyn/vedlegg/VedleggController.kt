package no.nav.sosialhjelp.innsyn.vedlegg

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.MeterRegistry
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
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
    meterRegistry: MeterRegistry,
) {
    val counter = meterRegistry.counter("vedlegg_size")

    // Send alle opplastede vedlegg for fiksDigisosId til Fiks
    @PostMapping("/{fiksDigisosId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun sendVedlegg(
        @PathVariable fiksDigisosId: String,
        @RequestPart("files") rawFiles: List<MultipartFile>,
    ): List<OppgaveOpplastingResponse> {
        counter.increment(rawFiles.sumOf { it.size }.toDouble())log.info("Forsøker å starter ettersendelse")
        val token = TokenUtils.getToken()
        tilgangskontroll.sjekkTilgang()

        val (metadata, files) = getMetadataAndRemoveFromFileList(rawFiles)

        check(files.isNotEmpty()) { "Ingen filer i forsendelse" }

        metadata.flatMap { it.filer }.onEach { fil ->
            fil.fil = files.find {
                it.originalFilename?.contains(fil.uuid.toString()) ?: false
            } ?: error("Fil i metadata var ikke i listen over filer")
        }

        val oppgaveValideringList =
            vedleggOpplastingService.sendVedleggTilFiks(fiksDigisosId, metadata, token)
        return mapToResponse(oppgaveValideringList)
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
