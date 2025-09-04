package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.app.exceptions.NotFoundException
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues
import no.nav.sosialhjelp.innsyn.vedlegg.calculateContentLength
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.util.UUID

interface MellomlagerService {
    suspend fun getAllDocumentMetadataForRef(navEksternRef: UUID): List<MellomlagringDokumentInfo>

    suspend fun getDocument(
        navEksternRef: UUID,
        documentId: UUID,
    ): ByteArray

    suspend fun deleteDocument(
        navEksternRef: UUID,
        documentId: UUID,
    )

    suspend fun deleteAllDocumentsForRef(navEksternRef: UUID)

    suspend fun processDocumentUpload(
        navEksternRef: UUID,
        allFiles: List<FilePart>,
    ): DocumentReferences
}

@Service
class MellomlagerServiceImpl(
    private val mellomlagerClient: MellomlagerClient,
    private val virusScanner: VirusScanner,
) : MellomlagerService {
    private val documentUploadHelper = DocumentUploadHelper()

    override suspend fun getAllDocumentMetadataForRef(navEksternRef: UUID): List<MellomlagringDokumentInfo> =
        mellomlagerClient
            .getDocumentMetadataForRef(navEksternRef)
            .let { response ->
                when (response) {
                    is MellomlagerResponse.MellomlagringDto -> response.mellomlagringMetadataList
                    is MellomlagerResponse.FiksError -> handleError(response)
                    else -> error("Unexpected response type: $response")
                }
            }

    override suspend fun getDocument(
        navEksternRef: UUID,
        documentId: UUID,
    ): ByteArray =
        mellomlagerClient
            .getDocument(navEksternRef, documentId)
            .let { response ->
                when (response) {
                    is MellomlagerResponse.ByteArrayResponse -> response.data
                    is MellomlagerResponse.FiksError -> handleError(response)
                    else -> error("Unexpected response type: $response")
                }
            }

    override suspend fun deleteDocument(
        navEksternRef: UUID,
        documentId: UUID,
    ) {
        mellomlagerClient
            .deleteDocument(navEksternRef, documentId)
            .also { response ->
                when (response) {
                    is MellomlagerResponse.EmptyResponse -> logger.info("Deleted document $documentId")
                    is MellomlagerResponse.FiksError -> handleError(response)
                    else -> error("Unexpected response type: $response")
                }
            }
    }

    override suspend fun deleteAllDocumentsForRef(navEksternRef: UUID) {
        mellomlagerClient
            .getDocumentMetadataForRef(navEksternRef)
            .also { response ->
                when (response) {
                    is MellomlagerResponse.EmptyResponse -> logger.info("Deleted all documents for ref $navEksternRef")
                    is MellomlagerResponse.FiksError -> handleError(response)
                    else -> error("Unexpected response type: $response")
                }
            }
    }

    override suspend fun processDocumentUpload(
        navEksternRef: UUID,
        allFiles: List<FilePart>,
    ): DocumentReferences {
        allFiles.doVirusScan()

        val metadata =
            documentUploadHelper.extractMetadataAndAddFiles(allFiles).firstOrNull()
                ?: error("Missing metadata.json for Klage upload")

        val validation = documentUploadHelper.validateMetadata(metadata)

        if (validation.filer.any { it.status.result != ValidationValues.OK }) {
            logger.error(
                "On file upload Klage - Validation failed for file(s): " +
                    "${validation.filer.filter { it.status.result != ValidationValues.OK }}",
            )
            throw FileValidationException("Upload document for Klage failed due to validation errors.")
        }

        return documentUploadHelper
            .createFilerForOpplasting(metadata)
            .let { mellomlagerClient.uploadDocuments(navEksternRef, it) }
            .let { mellomlagerResponse ->
                when (mellomlagerResponse) {
                    is MellomlagerResponse.MellomlagringDto -> mellomlagerResponse.toDocumentRefs()
                    is MellomlagerResponse.FiksError -> handleError(mellomlagerResponse)
                    else -> error("Unexpected response type: $mellomlagerResponse")
                }
            }
    }

    private suspend fun List<FilePart>.doVirusScan() {
        forEach { filePart ->
            virusScanner.scan(
                filePart.filename(),
                data = filePart,
                size = filePart.calculateContentLength(),
            )
        }
    }

    private fun handleError(error: MellomlagerResponse.FiksError): Nothing {
        when (error.status) {
            404 -> throw NotFoundException(error.message)
            else -> throw MellomlagerException("Noe feilet: $error")
        }
    }

    companion object {
        private val logger by logger()
    }
}

private fun MellomlagerResponse.MellomlagringDto.toDocumentRefs(): DocumentReferences =
    DocumentReferences(
        this.mellomlagringMetadataList.map { DocumentRef(it.filId, it.filnavn) },
    )

data class FileValidationException(
    override val message: String,
) : RuntimeException(message)

data class MellomlagerException(
    override val message: String,
) : RuntimeException(message)
