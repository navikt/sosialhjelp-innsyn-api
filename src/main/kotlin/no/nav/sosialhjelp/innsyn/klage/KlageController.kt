package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.util.UUID

@RestController
@RequestMapping("/api/v1/innsyn")
@ConditionalOnBean(KlageService::class)
class KlageController(
    private val klageService: KlageService,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @PostMapping("/{fiksDigisosId}/{klageId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun lastOppVedlegg(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
        @RequestPart("files") rawFiles: Flux<FilePart>,
    ): DocumentReferences {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        return klageService.lastOppVedlegg(
            fiksDigisosId = fiksDigisosId,
            klageId = klageId,
            rawFiles = rawFiles,
        )
    }

    @PostMapping("/{fiksDigisosId}/klage/send")
    suspend fun sendKlage(
        @PathVariable fiksDigisosId: UUID,
        @RequestBody input: KlageInput,
    ) {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        klageService.sendKlage(
            fiksDigisosId = fiksDigisosId,
            input = input,
        )
    }

    @GetMapping("/{fiksDigisosId}/klage/{vedtakId}")
    suspend fun hentKlage(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable vedtakId: UUID,
    ): KlageDto {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        return klageService.hentKlage(fiksDigisosId, vedtakId)?.toKlageDto()
            ?: throw NotFoundException("Klage for vedtakId $vedtakId ikke funnet for fiksDigisosId $fiksDigisosId")
    }

    @GetMapping("/{fiksDigisosId}/klager")
    suspend fun hentKlager(
        @PathVariable fiksDigisosId: UUID,
    ): KlagerDto {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        return KlagerDto(klager = klageService.hentKlager(fiksDigisosId).map { it.toKlageDto() })
    }

    private fun validateNotProd() {
        if (MiljoUtils.isRunningInProd()) throw IllegalStateException("KlageController should not be used in production environment")
    }
}

data class DocumentReferences (
    val documents: List<DocumentRef>,
)

data class DocumentRef(
    val documentId: UUID,
    val filename: String,
)

private fun Klage.toKlageDto() =
    KlageDto(
        klageId = klageId,
        vedtakId = vedtakId,
        klageTekst = klageTekst,
        status = status,
    )

data class KlageInput(
    val klageId: UUID,
    val vedtakId: UUID,
    val klageTekst: String,
)

data class KlagerDto(
    val klager: List<KlageDto>,
)

// TODO Hva skal legges ved i denne? Kun json, pdf,?
data class KlageDto(
//    val klageUrl: FilUrl,
    val klageId: UUID,
    val vedtakId: UUID,
    val klageTekst: String,
    val status: KlageStatus,
)
