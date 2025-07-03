package no.nav.sosialhjelp.innsyn.klage

import java.util.UUID
import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sosialhjelp.innsyn.vedlegg.OppgaveValidering
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service

interface MellomlagerService {
    suspend fun processFileUpload(klageId: UUID, metadata: OpplastetVedleggMetadata): List<OppgaveValidering>
}

@Service
class LocalMellomlagerService (
    private val mellomlagerClient: FiksMellomlagerClient,
    private val virusScanner: VirusScanner
): MellomlagerService {

    override suspend fun processFileUpload(
        klageId: UUID,
        metadata: OpplastetVedleggMetadata
    ): List<OppgaveValidering> {

        val validations = FilesValidator(virusScanner, listOf(metadata)).validate()
        if (validations.flatMap { validation -> validation.filer.map { it.status } }.any { it.result != ValidationValues.OK }) {
            return validations
        }

        mellomlagerClient.lastOppVedlegg(
            klageId = klageId,
            filerForOpplasting = metadata.toFilOpplasting()
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

            }
            .awaitSingleOrNull()


    }

    return emptyList()

}


