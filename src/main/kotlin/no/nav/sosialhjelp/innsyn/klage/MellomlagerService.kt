package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues
import no.nav.sosialhjelp.innsyn.vedlegg.calculateContentLength
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

interface MellomlagerService {
    suspend fun getAllDocumentMetadataForRef(
        navEksternRef: UUID,
    ): List<MellomlagringDokumentInfo>

    suspend fun getDocument(
        navEksternRef: UUID,
        documentId: UUID,
    ): ByteArray

    suspend fun deleteDocument(
        navEksternRef: UUID,
        documentId: UUID,
    )

    suspend fun deleteAllDocumentsForRef(navEksternRef: UUID,)

    suspend fun processDocumentUpload(
        navEksternRef: UUID,
        allFiles: List<FilePart>,
    ): DocumentReferences
}

@Service
class MellomlagerServiceImpl(
    private val mellomlagerClient: MellomlagerClient,
    private val virusScanner: VirusScanner,
): MellomlagerService {

    private val documentUploadHelper = DocumentUploadHelper()


    override suspend fun getAllDocumentMetadataForRef(navEksternRef: UUID): List<MellomlagringDokumentInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getDocument(navEksternRef: UUID, documentId: UUID): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun deleteDocument(navEksternRef: UUID, documentId: UUID) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllDocumentsForRef(navEksternRef: UUID) {
        TODO("Not yet implemented")
    }

    override suspend fun processDocumentUpload(
        navEksternRef: UUID,
        allFiles: List<FilePart>,
    ): DocumentReferences {

        allFiles.doVirusScan()

        val metadata = documentUploadHelper.extractMetadataAndAddFiles(allFiles).firstOrNull()
            ?: error("Missing metadata.json for Klage upload")

        val validation = documentUploadHelper.validateMetadata(metadata)

        if (validation.filer.any { it.status.result != ValidationValues.OK }) {
            logger.error(
                "On file upload Klage - Validation failed for file(s): " +
                        "${validation.filer.filter { it.status.result != ValidationValues.OK }}"
            )
            throw FileValidationException("Upload document for Klage failed due to validation errors.")
        }

        return documentUploadHelper.createFilerForOpplasting(metadata)
            .let { mellomlagerClient.uploadDocuments(navEksternRef, it) }
            .toDocumentRefs()
    }

    private suspend fun List<FilePart>.doVirusScan() {
        forEach { filePart ->
            virusScanner.scan(
                filePart.filename(),
                data = filePart,
                size = filePart.calculateContentLength()
            )
        }
    }

    companion object {
        private val logger by logger()
    }
}

private fun MellomlagringDto.toDocumentRefs(): DocumentReferences =
    DocumentReferences(
        this.mellomlagringMetadataList.map { DocumentRef(it.filId, it.filnavn) }
    )

data class FileValidationException(
    override val message: String,
): RuntimeException(message)
