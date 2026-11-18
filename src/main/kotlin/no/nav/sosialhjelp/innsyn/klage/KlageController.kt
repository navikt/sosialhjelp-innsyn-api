package no.nav.sosialhjelp.innsyn.klage

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/innsyn")
@ConditionalOnBean(KlageService::class)
class KlageController(
    private val klageUseCaseHandler: KlageUseCaseHandler,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @GetMapping("/{fiksDigisosId}/klage/tilgjengelig")
    suspend fun kanSendeKlage(
        @PathVariable fiksDigisosId: UUID,
    ): KlageTilgjengeligDto {
        tilgangskontroll.sjekkTilgang()

        return KlageTilgjengeligDto(klageUseCaseHandler.kanSendeKlage(fiksDigisosId))
    }

    @PostMapping("/{fiksDigisosId}/klage/send")
    suspend fun sendKlage(
        @PathVariable fiksDigisosId: UUID,
        @RequestBody input: KlageInput,
    ) {
        tilgangskontroll.sjekkTilgang()

        if (input.tekst.length > 2000) {
            throw KlageValidationException("Klage-tekst kan ikke være lengre enn 2000 tegn", KlageValidationException.Reason.TEKST_FOR_LANG)
        }

        klageUseCaseHandler.sendKlage(
            digisosId = fiksDigisosId,
            input = input,
        )
    }

    @GetMapping("/{fiksDigisosId}/klage/{klageId}")
    suspend fun hentKlage(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ): KlageDto? {
        tilgangskontroll.sjekkTilgang()

        return klageUseCaseHandler.hentKlage(fiksDigisosId, klageId)
    }

    @GetMapping("/{fiksDigisosId}/{vedtakId}/klage")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Klage for vedtaket"),
        ApiResponse(responseCode = "404", description = "Ingen klage er sendt for dette vedtaket", content = [Content()]),
    )
    suspend fun hentKlageForVedtak(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable vedtakId: UUID,
    ): ResponseEntity<KlageDto> {
        tilgangskontroll.sjekkTilgang()

        return klageUseCaseHandler
            .hentKlageForVedtak(fiksDigisosId, vedtakId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/{fiksDigisosId}/klager")
    suspend fun hentKlager(
        @PathVariable fiksDigisosId: UUID,
    ): List<KlageRef> {
        tilgangskontroll.sjekkTilgang()

        return klageUseCaseHandler.hentAlleKlager(fiksDigisosId)
    }
}

data class DocumentsForKlage(
    val documents: List<DocumentForKlage>,
)

data class DocumentForKlage(
    val klageId: UUID,
    val documentId: UUID,
    val filename: String,
)

data class KlageInput(
    val klageId: UUID,
    val vedtakId: UUID,
    val tekst: String,
)

data class KlageRef(
    val klageId: UUID,
    val vedtakId: UUID,
    val fiksDigisosId: UUID,
    val timestampSendt: Long? = null,
)

data class KlageDto(
    val digisosId: UUID,
    val klageId: UUID,
    val vedtakId: UUID,
    val klagePdf: VedleggResponse,
    val opplastedeVedlegg: List<VedleggResponse> = emptyList(),
    val ettersendelser: List<EttersendelseDto> = emptyList(),
    val timestampSendt: Long,
)

data class EttersendelseDto(
    val navEksternRefId: UUID,
    val vedlegg: List<VedleggResponse> = emptyList(),
    val timestampSendt: Long,
)
