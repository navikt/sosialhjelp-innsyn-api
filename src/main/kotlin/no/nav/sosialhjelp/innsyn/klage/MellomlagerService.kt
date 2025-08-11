package no.nav.sosialhjelp.innsyn.klage

import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sosialhjelp.innsyn.vedlegg.OppgaveValidering
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import java.util.UUID
import org.springframework.context.annotation.Profile
import org.springframework.http.codec.multipart.FilePart

interface MellomlagerService {
    suspend fun processFileUpload(
        klageId: UUID,
        allFiles: List<FilePart>,
    ): List<OppgaveValidering>
}

@Profile("!local")
@Service
class MellomlagerServiceImpl(
    private val mellomlagerClient: MellomlagerClient,
    private val virusScanner: VirusScanner,
): MellomlagerService {

    private val uploadDocumentHelper = UploadDocumentHelper(virusScanner)

    override suspend fun processFileUpload(
        klageId: UUID,
        allFiles: List<FilePart>,
    ): List<OppgaveValidering> {

        val metadataList = uploadDocumentHelper.extractAndAddFilesToMetadata(allFiles)

        val validations = uploadDocumentHelper.validateMetadata(metadataList)
        if (validations.flatMap { validation -> validation.filer.map { it.status } }.any { it.result != ValidationValues.OK }) {
            return validations
        }

        val filerForOpplasting = uploadDocumentHelper.createFilerForOpplasting(metadataList)


        mellomlagerClient.lastOppVedlegg(klageId, filerForOpplasting)

        // TODO Do upload


        return validations
    }




















}

@Profile("local")
@Service
class LocalMellomlagerService(
    private val mellomlagerClient: MellomlagerClient,
    private val virusScanner: VirusScanner,
) : MellomlagerService {
    override suspend fun processFileUpload(
        klageId: UUID,
        metadata: OpplastetVedleggMetadata,
    ): List<OppgaveValidering> {
        val validations = FilesValidator(virusScanner, listOf(metadata)).validate()
        if (validations.flatMap { validation -> validation.filer.map { it.status } }.any { it.result != ValidationValues.OK }) {
            return validations
        }

        mellomlagerClient.lastOppVedlegg(
            klageId = klageId,
            filerForOpplasting = metadata.toFilOpplasting(),
        )

        return validations
    }
}

private suspend fun OpplastetVedleggMetadata.toFilOpplasting(): List<FilOpplasting> {
    filer.map { opplastetFil ->

        val content = opplastetFil.fil.content()
        DataBufferUtils
            .join { content }
            .map {
                val bytes = ByteArray(it.readableByteCount())
                it.read(bytes)
                DataBufferUtils.release(it)
            }.awaitSingleOrNull()
    }

    return emptyList()
}
