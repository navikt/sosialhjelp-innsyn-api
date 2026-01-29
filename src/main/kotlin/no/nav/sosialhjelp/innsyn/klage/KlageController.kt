package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
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
    private val klageUseCaseHandler: KlageUseCaseHandler,
    private val tilgangskontroll: TilgangskontrollService,
) {
    @PostMapping("/{fiksDigisosId}/{navEksternRefId}/vedlegg", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadDocuments(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable navEksternRefId: UUID,
        @RequestPart("files") rawFiles: Flux<FilePart>,
    ): DocumentsForKlage {
        // TODO En eller annen form for sjekk av fiksDigisosId ?
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        return klageUseCaseHandler.lastOppVedlegg(
            digisosId = fiksDigisosId,
            navEksternRefId = navEksternRefId,
            rawFiles = rawFiles,
        )
    }

    @GetMapping("/{fiksDigisosId}/klage/{klageId}/avbryt")
    suspend fun cancelKlage(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ) {
        TODO("Implementer avbrytKlage i KlageController")

        // TODO Slette eventuelle vedlegg i mellomlageret?
    }

    @PostMapping("/{fiksDigisosId}/klage/send")
    suspend fun sendKlage(
        @PathVariable fiksDigisosId: UUID,
        @RequestBody input: KlageInput,
    ) {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        klageUseCaseHandler.sendKlage(
            digisosId = fiksDigisosId,
            input = input,
        )
    }

    @PostMapping("/{fiksDigisosId}/klage/{klageId}/ettersendelse/{ettersendelseId}")
    suspend fun sendEttersendelse(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
        @PathVariable ettersendelseId: UUID,
    ) {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        klageUseCaseHandler.sendEttersendelse(fiksDigisosId, klageId, ettersendelseId)
    }

    @GetMapping("/{fiksDigisosId}/klage/{klageId}")
    suspend fun hentKlage(
        @PathVariable fiksDigisosId: UUID,
        @PathVariable klageId: UUID,
    ): KlageDto? {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        return klageUseCaseHandler.hentKlage(fiksDigisosId, klageId)
    }

    @GetMapping("/{fiksDigisosId}/klager")
    suspend fun hentKlager(
        @PathVariable fiksDigisosId: UUID,
    ): List<KlageRef> {
        validateNotProd()
        tilgangskontroll.sjekkTilgang()

        return klageUseCaseHandler.hentAlleKlager(fiksDigisosId)
    }

    private fun validateNotProd() {
        if (MiljoUtils.isRunningInProd()) throw IllegalStateException("KlageController should not be used in production environment")
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
)

// TODO Hva skal legges ved i denne? Kun json, pdf,?
// TODO Denne trenger URL til klage.pdf
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
