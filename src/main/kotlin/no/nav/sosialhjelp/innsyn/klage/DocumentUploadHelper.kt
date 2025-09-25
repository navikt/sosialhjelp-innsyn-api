package no.nav.sosialhjelp.innsyn.klage

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.FilValidering
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.MAKS_TOTAL_FILSTORRELSE
import no.nav.sosialhjelp.innsyn.vedlegg.OppgaveValidering
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetFil
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.vedlegg.TikaFileType
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationResult
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues
import no.nav.sosialhjelp.innsyn.vedlegg.createFilename
import no.nav.sosialhjelp.innsyn.vedlegg.mapToTikaFileType
import no.nav.sosialhjelp.innsyn.vedlegg.splitFileName
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.tika.Tika
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

class DocumentUploadHelper {
    companion object {
        private val logger by logger()
    }

    suspend fun extractMetadataAndAddFiles(allFiles: List<FilePart>): List<OpplastetVedleggMetadata> {
        val metadataList = extractMetadata(allFiles)
        val files = extractOtherFiles(allFiles)

        files.checkAllFilesHasMatch(metadataList.flatMap { it.filer })
        addFileToMatchingMetadata(metadataList, files)

        return metadataList
    }

    suspend fun validateMetadata(metadata: OpplastetVedleggMetadata): OppgaveValidering = metadata.validate().await()

    suspend fun createFilerForOpplasting(metadata: OpplastetVedleggMetadata): List<FilForOpplasting> =
        metadata.filer
            .map { file ->
                file.filnavn = file.createFilename()
                val dataBuffer = DataBufferUtils.join(file.fil.content()).awaitSingle()

                FilForOpplasting(
                    filnavn = file.filnavn,
                    mimetype = file.getMimeType(),
                    storrelse = file.size(),
                    data = dataBuffer.asInputStream(),
                )
            }

    private suspend fun extractMetadata(files: List<FilePart>): List<OpplastetVedleggMetadata> =
        files
            .firstOrNull { it.filename() == "metadata.json" }
            ?.content()
            ?.let { DataBufferUtils.join(it) }
            ?.map {
                val bytes = ByteArray(it.readableByteCount())
                it.read(bytes)
                DataBufferUtils.release(it)

                objectMapper.readValue<List<OpplastetVedleggMetadata>>(bytes)
            }?.awaitSingleOrNull()
            ?.filter { it.filer.isNotEmpty() }
            ?: error("Missing metadata.json for Klage upload")

    private fun OpplastetFil.getMimeType() =
        when (this.tikaMimeType) {
            "text/x-matlab" -> "application/pdf"
            else -> this.tikaMimeType
        }

    private fun extractOtherFiles(files: List<FilePart>): List<FilePart> =
        files
            .filterNot { it.filename() == "metadata.json" }
            .also {
                check(it.isNotEmpty()) { "Ingen filer i forsendelse" }
                check(it.size <= 30) { "Over 30 filer i forsendelse: ${it.size} filer" }
            }

    private fun List<FilePart>.checkAllFilesHasMatch(fileRefs: List<OpplastetFil>) {

        all { file -> file.filename().substringBefore(".") in fileRefs.map { ref -> ref.uuid.toString() } }
            .also { allFilesHasMatch ->
                require(allFilesHasMatch) {
                    "Ikke alle filer i metadata.json ble funnet i forsendelsen"
                }
            }
    }

    private fun addFileToMatchingMetadata(
        metadataList: List<OpplastetVedleggMetadata>,
        files: List<FilePart>,
    ) {
        files.forEach { file ->
            metadataList
                .flatMap { it.filer }
                .find { file.filename().contains(it.uuid.toString()) }
                ?.apply { fil = file }
        }
    }

    private suspend fun OpplastetVedleggMetadata.validate() =
        coroutineScope {
            async {
                OppgaveValidering(
                    type,
                    tilleggsinfo,
                    innsendelsesfrist,
                    hendelsetype,
                    hendelsereferanse,
                    filer.map { file ->
                        val validationResult = file.validate()
                        FilValidering(file.filnavn.sanitize(), validationResult).also { file.validering = it }
                    },
                )
            }
        }

    private suspend fun OpplastetFil.validate(): ValidationResult {
        if (size() > MAKS_TOTAL_FILSTORRELSE) {
            return ValidationResult(ValidationValues.FILE_TOO_LARGE)
        }

        if (filnavn.containsIllegalCharacters()) {
            return ValidationResult(ValidationValues.ILLEGAL_FILENAME)
        }

        val result = validateFileType()

        return result
    }

    private suspend fun OpplastetFil.validateFileType(): ValidationResult {
        detectAndSetMimeType()
        if (tikaMimeType == "text/x-matlab") {
            logger.info(
                "Tika detekterte mimeType text/x-matlab. " +
                    "Vi antar at dette egentlig er en PDF, men som ikke har korrekte magic bytes (%PDF).",
            )
        }
        val fileType = mapToTikaFileType(tikaMimeType)

        if (fileType == TikaFileType.UNKNOWN) {
            val firstBytes =
                DataBufferUtils.join(fil.content()).awaitFirstOrNull()?.let { dataBuffer ->
                    val size = dataBuffer.readableByteCount().coerceAtMost(8)
                    ByteArray(size).also {
                        dataBuffer.read(it, 0, size)
                    }
                }

            logger.warn(
                "Fil validert som TikaFileType.UNKNOWN. Men har " +
                    "\r\nextension: \"${splitFileName(fil.filename()).extension}\"," +
                    "\r\nvalidatedFileType: ${fileType.name}," +
                    "\r\ntikaMediaType: $tikaMimeType," +
                    "\r\nmime: ${fil.headers().contentType}" +
                    ",\r\nførste bytes: $firstBytes",
            )
            return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
        }
        if (fileType == TikaFileType.PDF) {
            val dataBuffer = DataBufferUtils.join(fil.content()).awaitSingle()
            val inputStream = dataBuffer.asInputStream()
            return ValidationResult(inputStream.use { it.checkIfPdfIsValid() }, TikaFileType.PDF)
        }
        if (fileType == TikaFileType.JPEG || fileType == TikaFileType.PNG) {
            val ext: String = fil.filename().substringAfterLast(".")
            if (ext.lowercase() in listOf("jfif", "pjpeg", "pjp")) {
                logger.warn(
                    "Fil validert som TikaFileType.$fileType. Men filnavn slutter på $ext, som er en av filtypene vi pt ikke godtar.",
                )
                return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
            }
        }
        return ValidationResult(ValidationValues.OK, fileType)
    }

    private suspend fun OpplastetFil.detectAndSetMimeType() {
        withContext(Dispatchers.IO) {
            val dataBuffer = DataBufferUtils.join(fil.content()).awaitSingle()
            val inputStream = dataBuffer.asInputStream()

            try {
                Tika().detect(inputStream).also {
                    tikaMimeType = it
                }
            } finally {
                DataBufferUtils.release(dataBuffer)
                inputStream.close()
            }
        }
    }

    private suspend fun InputStream.checkIfPdfIsValid(): ValidationValues =
        withContext(Dispatchers.IO) {
            val randomAccessReadBuffer = RandomAccessReadBuffer(this@checkIfPdfIsValid)
            try {
                Loader
                    .loadPDF(randomAccessReadBuffer)
                    .use { document ->
                        if (document.isEncrypted) {
                            ValidationValues.PDF_IS_ENCRYPTED
                        }
                        ValidationValues.OK
                    }
            } catch (e: InvalidPasswordException) {
                logger.warn(ValidationValues.PDF_IS_ENCRYPTED.name + " " + e.message)
                ValidationValues.PDF_IS_ENCRYPTED
            } catch (e: IOException) {
                logger.warn(ValidationValues.COULD_NOT_LOAD_DOCUMENT.name, e)
                ValidationValues.COULD_NOT_LOAD_DOCUMENT
            } finally {
                randomAccessReadBuffer.close()
                close()
            }
        }
}

data class OpplastetVedleggMetadataRequest(
    val type: String,
    val tilleggsinfo: String?,
    val hendelsetype: JsonVedlegg.HendelseType?,
    val hendelsereferanse: String?,
    val filer: List<OpplastetFilMetadata>,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val innsendelsesfrist: LocalDate?,
)

data class OpplastetFilMetadata(
    val filnavn: String,
    val uuid: UUID,
)

private fun OpplastetVedleggMetadataRequest.toOpplastetVedleggMetadata() =
    OpplastetVedleggMetadata(
        type = type,
        tilleggsinfo = tilleggsinfo,
        hendelsetype = hendelsetype,
        hendelsereferanse = hendelsereferanse,
        filer = filer.map { OpplastetFil(Filename(it.filnavn), it.uuid) }.toMutableList(),
        innsendelsesfrist = innsendelsesfrist,
    )
