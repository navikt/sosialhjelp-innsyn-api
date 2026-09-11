package no.nav.sosialhjelp.innsyn.vedlegg

import com.fasterxml.jackson.annotation.JsonFormat
import kotlinx.coroutines.reactor.awaitSingle
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/innsyn")
class VedleggController(
    private val vedleggService: VedleggService,
    private val clientProperties: ClientProperties,
    private val tilgangskontroll: TilgangskontrollService,
    private val eventService: EventService,
    private val fiksService: FiksService,
) {
    @GetMapping("/{fiksDigisosId}/vedlegg", produces = ["application/json;charset=UTF-8"])
    suspend fun hentVedlegg(
        @PathVariable fiksDigisosId: String,
    ): ResponseEntity<List<VedleggResponse>> {
        tilgangskontroll.sjekkTilgang()
        val digisosSak = fiksService.getSoknad(fiksDigisosId)
        val model = eventService.createModel(digisosSak)

        val internalVedleggList: List<InternalVedlegg> = vedleggService.hentAlleOpplastedeVedlegg(digisosSak, model)
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
        private const val LENGTH_OF_UUID_PART = 9
    }
}

data class OpplastetVedleggMetadata(
    val type: String,
    val tilleggsinfo: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: MutableList<OpplastetFil>,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
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

suspend fun FilePart.calculateContentLength(): Long {
    val dataBuffer = DataBufferUtils.join(content()).awaitSingle()
    val contentLength = dataBuffer.readableByteCount().toLong()
    DataBufferUtils.release(dataBuffer)
    return contentLength
}
