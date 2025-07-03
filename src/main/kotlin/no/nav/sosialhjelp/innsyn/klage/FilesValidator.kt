package no.nav.sosialhjelp.innsyn.klage

import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.vedlegg.FilValidering
import no.nav.sosialhjelp.innsyn.vedlegg.MAKS_TOTAL_FILSTORRELSE
import no.nav.sosialhjelp.innsyn.vedlegg.OppgaveValidering
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetFil
import no.nav.sosialhjelp.innsyn.vedlegg.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.vedlegg.TikaFileType
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationResult
import no.nav.sosialhjelp.innsyn.vedlegg.ValidationValues
import no.nav.sosialhjelp.innsyn.vedlegg.mapToTikaFileType
import no.nav.sosialhjelp.innsyn.vedlegg.splitFileName
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.tika.Tika
import org.springframework.core.io.buffer.DataBufferUtils

class FilesValidator(
    private val virusScanner: VirusScanner,
    private val metadatas: List<OpplastetVedleggMetadata>,
) {

    suspend fun validate() = metadatas.validate().awaitAll()

    private suspend fun List<OpplastetVedleggMetadata>.validate() =
        coroutineScope {
            map { metadata ->
                async {
                    OppgaveValidering(
                        metadata.type,
                        metadata.tilleggsinfo,
                        metadata.innsendelsesfrist,
                        metadata.hendelsetype,
                        metadata.hendelsereferanse,
                        metadata.filer.map { file ->
                            val validationResult = file.validate()
                            FilValidering(file.filnavn.sanitize(), validationResult).also { file.validering = it }
                        },
                    )
                }
            }
        }

    private suspend fun OpplastetFil.validate(): ValidationResult {
        if (size() > MAKS_TOTAL_FILSTORRELSE) {
            return ValidationResult(ValidationValues.FILE_TOO_LARGE)
        }

        if (filnavn.containsIllegalCharacters()) {
            return ValidationResult(ValidationValues.ILLEGAL_FILENAME)
        }
        virusScanner.scan(filnavn.value, fil, size())

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
                logger.warn("Fil validert som TikaFileType.$fileType. Men filnavn slutter på $ext, som er en av filtypene vi pt ikke godtar.")
                return ValidationResult(ValidationValues.ILLEGAL_FILE_TYPE)
            }
        }
        return ValidationResult(ValidationValues.OK, fileType)
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

    companion object {
        private val logger by logger()
    }
}
