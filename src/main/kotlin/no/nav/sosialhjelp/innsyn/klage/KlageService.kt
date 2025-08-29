package no.nav.sosialhjelp.innsyn.klage

import java.io.ByteArrayInputStream
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.PdfGenerator
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

interface KlageService {
    suspend fun sendKlage(
        fiksDigisosId: UUID,
        input: KlageInput,
    )

    suspend fun hentKlager(fiksDigisosId: UUID): List<FiksKlageDto>

    suspend fun hentKlage(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): FiksKlageDto?

    suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentReferences
}

@Service
class KlageServiceImpl(
    private val klageClient: FiksKlageClient,
    private val mellomlagerService: MellomlagerService,
    private val fiksClient: FiksClient,
    private val eventService: EventService,
) : KlageService {

    override suspend fun sendKlage(
        fiksDigisosId: UUID,
        input: KlageInput,
    ) {
        klageClient.sendKlage(
            digisosId = fiksDigisosId,
            klageId = input.klageId,
            MandatoryFilesForKlage(
                klageJson = input.toJson(),
                klagePdf = input.createKlagePdf(),
                vedleggJson = input.createJsonVedleggSpec(),
            )
        )
    }

    override suspend fun hentKlager(fiksDigisosId: UUID): List<FiksKlageDto> = klageClient.hentKlager(fiksDigisosId)

    override suspend fun hentKlage(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): FiksKlageDto? {
        val klager = klageClient.hentKlager(digisosId = fiksDigisosId)

        validateVedtakExists(fiksDigisosId, vedtakId)

        TODO ("Pr nå ingen kobling mellom vedtak og klage")
    }

    private suspend fun validateVedtakExists(fiksDigisosId: UUID, vedtakId: UUID) {
        fiksClient.hentDigisosSak(fiksDigisosId.toString())
            .let { digisosSak -> eventService.createModel(digisosSak) }
            .also { internalDigisosSoker -> internalDigisosSoker.validateVedtakExists(vedtakId) }
    }

    override suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentReferences {
        val allFiles = rawFiles.asFlow().toList()

        return mellomlagerService.processDocumentUpload(klageId, allFiles)
    }

    private suspend fun KlageInput.createJsonVedleggSpec(): JsonVedleggSpesifikasjon {

        val allMetadata = mellomlagerService.getAllDocumentMetadataForRef(klageId)

        // TODO Hva forventes her i kontekst av klage?
        return JsonVedlegg()
            .withType("klage")
            .withStatus(if (allMetadata.isNotEmpty()) "LASTET_OPP" else "INGEN_VEDLEGG")
            .withTilleggsinfo("tilleggsinfo")
            .withHendelseType(JsonVedlegg.HendelseType.BRUKER)
            .withHendelseReferanse(this.vedtakId.toString())
            .withKlageId(klageId.toString())
            .withFiler(allMetadata.map { JsonFiler().withFilnavn(it.filnavn) })
            .let { JsonVedleggSpesifikasjon().withVedlegg(listOf(it)) }
    }

    private fun KlageInput.toJson(): String = objectMapper.writeValueAsString(this)

    private fun KlageInput.createKlagePdf(): FilForOpplasting {
        return PDDocument()
            .use { document -> generateKlagePdf(document, this) }
            .let { pdf ->
                FilForOpplasting(
                    filnavn = Filename("klage.pdf"),
                    mimetype = "application/pdf",
                    storrelse = pdf.size.toLong(),
                    data = ByteArrayInputStream(pdf)
                )
            }
    }

    private fun generateKlagePdf(
        document: PDDocument,
        input: KlageInput,
    ): ByteArray =
        PdfGenerator(document)
            .run {
                addCenteredH1Bold("Klage på vedtak")
                addCenteredH4Bold("Vedtak: ${input.vedtakId}")
                addBlankLine()
                addCenteredH4Bold("Klage ID: ${input.klageId}")
                addText(input.tekst)
                finish()
            }
}

private fun InternalDigisosSoker.validateVedtakExists(vedtakId: UUID) {
    this.saker
        .flatMap { sak -> sak.vedtak }
        .find { vedtak -> vedtak.id == vedtakId.toString() }
        ?: throw IllegalArgumentException("Vedtak med id $vedtakId finnes ikke")
}
