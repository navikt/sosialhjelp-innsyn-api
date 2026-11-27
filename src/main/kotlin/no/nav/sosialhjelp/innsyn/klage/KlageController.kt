package no.nav.sosialhjelp.innsyn.klage

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.klage.draft.Klage
import no.nav.sosialhjelp.innsyn.klage.draft.KlageDraftUseCaseHandler
import no.nav.sosialhjelp.innsyn.klage.draft.KlageMetadata
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/innsyn")
class KlageController(
    private val sendKlageUseCaseHandler: SendKlageUseCaseHandler,
    private val klageUseCaseHandler: KlageUseCaseHandler,
    private val tilgangskontroll: TilgangskontrollService,
    private val klageDraftUseCaseHandler: KlageDraftUseCaseHandler,
) {
    @PostMapping("/{fiksDigisosId}/klage/{vedtakId}/opprett")
    suspend fun createDraft(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable vedtakId: UUID,
    ): OpprettetKlageDto {
        // TODO interceptor?
        tilgangskontroll.sjekkTilgang()

        return klageDraftUseCaseHandler
            .createKlage(
                digisosId = fiksDigisosId,
                vedtakId = vedtakId,
            ).toOpprettetKlageDto()
    }

    @PostMapping("/{fiksDigisosId}/klage/{klageId}/utkast")
    suspend fun getDraft(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ): KlageUtkastDto {
        tilgangskontroll.sjekkTilgang()

        return klageDraftUseCaseHandler
            .getDraft(klageId = klageId)
            ?.toKlageUtkastDto()
            ?: throw NotFoundException("Fant ikke klage med id $klageId")
    }

    @PostMapping("/{fiksDigisosId}/klage/{klageId}/begrunnelse/oppdater")
    suspend fun updateBegrunnelse(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
        @RequestBody input: KlageInput,
    ) {
        tilgangskontroll.sjekkTilgang()

        if (input.tekst.length > 2000) {
            throw KlageValidationException("Klage-tekst kan ikke være lengre enn 2000 tegn", KlageValidationException.Reason.TEKST_FOR_LANG)
        }

        klageDraftUseCaseHandler.updateDraft(
            klageId = klageId,
            begrunnelse = input.tekst,
        )
    }

    @PostMapping("/{fiksDigisosId}/klage/{klageId}/send")
    suspend fun sendDraft(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ) {
        tilgangskontroll.sjekkTilgang()

        sendKlageUseCaseHandler.sendKlage(
            digisosId = fiksDigisosId,
            klageId = klageId,
        )
    }

    @PostMapping("/{fiksDigisosId}/klage/{klageId}/slett")
    suspend fun deleteDraft(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ) {
        klageDraftUseCaseHandler.deleteDraft(
            klageId = klageId,
        )
    }

    @GetMapping("/{fiksDigisosId}/klage/tilgjengelig")
    suspend fun kanSendeKlage(
        @PathVariable fiksDigisosId: UUID,
    ): KlageTilgjengeligDto {
        tilgangskontroll.sjekkTilgang()

        return KlageTilgjengeligDto(klageDraftUseCaseHandler.kanSendeKlage(fiksDigisosId))
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

private fun Pair<KlageMetadata, Klage?>.toKlageUtkastDto(): KlageUtkastDto {
    this.let { (metadata, klage) ->
        return KlageUtkastDto(
            klageId = metadata.klageId,
            vedtakId = metadata.vedtakFilreferanseId,
            begrunnelse = klage?.begrunnelse,
            opprettet = metadata.opprettet,
        )
    }
}

private fun KlageMetadata.toOpprettetKlageDto() =
    OpprettetKlageDto(
        klageId = this.klageId,
        vedtakId = this.vedtakFilreferanseId,
        fiksDigisosId = this.digisosId,
        opprettet = this.opprettet,
    )

data class KlageUtkastDto(
    val klageId: UUID,
    val vedtakId: UUID,
    val begrunnelse: String?,
    val opprettet: LocalDateTime,
    val opplastedeVedlegg: List<VedleggResponse> = emptyList(),
)

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

data class OpprettetKlageDto(
    val klageId: UUID,
    val vedtakId: UUID,
    val fiksDigisosId: UUID,
    val opprettet: LocalDateTime,
)
