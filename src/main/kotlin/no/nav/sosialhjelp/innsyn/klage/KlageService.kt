package no.nav.sosialhjelp.innsyn.klage

import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.utils.hentDokumentlagerUrl
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
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
    ): KlageDto?

    suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage
}

@Service
class KlageServiceImpl(
    private val klageClient: FiksKlageClient,
    private val mellomlagerService: MellomlagerService,
    private val fiksClient: FiksClient,
    private val clientProperties: ClientProperties,
) : KlageService {
    override suspend fun sendKlage(
        fiksDigisosId: UUID,
        input: KlageInput,
    ) {
        klageClient.sendKlage(
            digisosId = fiksDigisosId,
            klageId = input.klageId,
            vedtakId = input.vedtakId,
            MandatoryFilesForKlage(
                klageJson = input.toJson(),
                klagePdf = input.createKlagePdf(),
                vedleggJson = input.createJsonVedleggSpec(),
            ),
        )
    }

    override suspend fun hentKlager(fiksDigisosId: UUID): List<FiksKlageDto> = klageClient.hentKlager(fiksDigisosId)

    override suspend fun hentKlage(
        fiksDigisosId: UUID,
        vedtakId: UUID,
    ): KlageDto? {
        val fiksKlage = klageClient.hentKlager(digisosId = fiksDigisosId).find { it.vedtakId == vedtakId }
            ?: return null

        val klagePdf = fiksKlage.klageDokument.toVedleggResponse(fiksKlage.getTidspunktSendt())
        val opplastedeVedlegg = fiksKlage.vedlegg.map { it.toVedleggResponse(fiksKlage.getTidspunktSendt()) }

        fiksClient.hentDokument(
            fiksDigisosId.toString(),
            fiksKlage.klageMetadata.toString(),
            JsonVedleggSpesifikasjon::class.java,
        )
            .also { vedleggSpec -> vedleggSpec.validerAllMatch(opplastedeVedlegg.map { it.filnavn }) }

        return KlageDto(
            digisosId = fiksDigisosId,
            klageId = fiksKlage.klageId,
            vedtakId = fiksKlage.vedtakId,
            klagePdf = klagePdf,
            status = fiksKlage.sendtKvittering.sendtStatus,
            opplastedeVedlegg = opplastedeVedlegg
        )
    }

    override suspend fun lastOppVedlegg(
        fiksDigisosId: UUID,
        klageId: UUID,
        rawFiles: Flux<FilePart>,
    ): DocumentsForKlage {
        val allFiles = rawFiles.asFlow().toList()

        return mellomlagerService.processDocumentUpload(klageId, allFiles)
    }

    private suspend fun KlageInput.createJsonVedleggSpec(): JsonVedleggSpesifikasjon {
        val allMetadata =
            runCatching { mellomlagerService.getAllDocumentMetadataForRef(klageId) }
                .getOrElse { ex ->
                    when (ex) {
                        is NotFoundException -> emptyList()
                        else -> throw ex
                    }
                }

        // TODO Hva forventes her i kontekst av klage?
        return JsonVedlegg()
            .withType("klage")
            .withStatus(if (allMetadata.isNotEmpty()) "LASTET_OPP" else "INGEN_VEDLEGG")
            .withHendelseType(JsonVedlegg.HendelseType.BRUKER)
            .withHendelseReferanse(this.vedtakId.toString())
            .withKlageId(klageId.toString())
            .withFiler(allMetadata.map { JsonFiler().withFilnavn(it.filnavn) })
            .let { JsonVedleggSpesifikasjon().withVedlegg(listOf(it)) }
    }

    private fun KlageInput.toJson(): String = objectMapper.writeValueAsString(this)

    private fun KlageInput.createKlagePdf(): FilForOpplasting =
        PDDocument()
            .use { document -> generateKlagePdf(document, this) }
            .let { pdf ->
                FilForOpplasting(
                    filnavn = Filename("klage.pdf"),
                    mimetype = "application/pdf",
                    storrelse = pdf.size.toLong(),
                    data = ByteArrayInputStream(pdf),
                )
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

    private fun DokumentInfoDto.toVedleggResponse(tidspunktSendt: LocalDateTime) = VedleggResponse(
        filnavn = filnavn,
        storrelse = storrelse,
        url = hentDokumentlagerUrl(clientProperties, dokumentlagerDokumentId.toString()),
        type = "klage_pdf",
        tilleggsinfo = null,
        datoLagtTil = tidspunktSendt
    )
}

private fun JsonVedleggSpesifikasjon.validerAllMatch(filnavnList: List<String>) {
    vedlegg
        .flatMap { jsonVedlegg -> jsonVedlegg.filer }
        .all { jsonFil -> filnavnList.contains(jsonFil.filnavn) }
        .also { allMatched -> if (!allMatched) error("Fant ikke alle alle filer i Mellomlager") }
}

private fun FiksKlageDto.getTidspunktSendt() = sendtKvittering.sendtStatus.timestamp.let { unixToLocalDateTime(it) }
