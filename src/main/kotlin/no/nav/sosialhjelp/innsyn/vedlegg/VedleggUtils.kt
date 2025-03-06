package no.nav.sosialhjelp.innsyn.vedlegg

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.collect
import org.apache.tika.Tika
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import java.io.InputStream
import java.io.SequenceInputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Collections
import kotlin.math.absoluteValue

suspend fun getSha512FromDataBuffer(filePart: FilePart?): String {
    if (filePart == null) {
        return ""
    }

    val md = MessageDigest.getInstance("SHA-512")

    filePart.content().collect { dataBuffer: DataBuffer ->
        val byteArray = ByteArray(dataBuffer.readableByteCount())
        dataBuffer.read(byteArray)
        md.update(byteArray)
        DataBufferUtils.release(dataBuffer)
    }

    val digest = md.digest()
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

fun sanitizeFileName(filename: String) = Normalizer.normalize(filename, Normalizer.Form.NFC).trim()

fun detectTikaType(inputStream: InputStream): String {
    return Tika().detect(inputStream)
}

fun mapToTikaFileType(tikaMediaType: String): TikaFileType {
    return when {
        tikaMediaType.equals("application/pdf", ignoreCase = true) -> TikaFileType.PDF
        tikaMediaType.equals("text/x-matlab", ignoreCase = true) -> TikaFileType.PDF
        tikaMediaType.equals("image/png", ignoreCase = true) -> TikaFileType.PNG
        tikaMediaType.equals("image/jpeg", ignoreCase = true) -> TikaFileType.JPEG
        else -> TikaFileType.UNKNOWN
    }
}

enum class TikaFileType {
    JPEG,
    PNG,
    PDF,
    UNKNOWN,
    ;

    fun toExt(): String =
        when (this) {
            JPEG -> ".jpg"
            PNG -> ".png"
            PDF -> ".pdf"
            UNKNOWN -> error("Unknown extension")
        }
}

fun splitFileName(fileName: String): FileNameSplit {
    val indexOfFileExtension = fileName.lastIndexOf(".")
    if (indexOfFileExtension != -1) {
        val ext = fileName.substring(indexOfFileExtension, fileName.length)
        if (ext in listOf(".jpg", ".jpeg", ".png", ".pdf")) {
            return FileNameSplit(fileName.substring(0, indexOfFileExtension), ext)
        }
    }
    return FileNameSplit(fileName, "")
}

class FileNameSplit(val name: String, val extension: String)

fun kombinerAlleLikeVedlegg(alleVedlegg: List<InternalVedlegg>): List<InternalVedlegg> {
    val kombinertListe = ArrayList<InternalVedlegg>()
    alleVedlegg.forEach {
        val funnet =
            kombinertListe.firstOrNull { kombinert ->
                (
                    areDatesWithinOneMinute(it.tidspunktLastetOpp, kombinert.tidspunktLastetOpp) &&
                        kombinert.type == it.type &&
                        kombinert.tilleggsinfo == it.tilleggsinfo
                )
            }
        if (funnet != null) {
            funnet.dokumentInfoList.addAll(it.dokumentInfoList)
        } else {
            kombinertListe.add(it)
        }
    }
    return kombinertListe
}

fun areDatesWithinOneMinute(
    firstDate: LocalDateTime?,
    secondDate: LocalDateTime?,
): Boolean {
    return (firstDate == null && secondDate == null) ||
        ChronoUnit.MINUTES.between(firstDate, secondDate).absoluteValue < 1
}

suspend fun Flow<DataBuffer>.size(): Long = fold(0L) { acc, it -> acc + it.readableByteCount() }

suspend fun Flow<DataBuffer>.asInputStream(): SequenceInputStream = SequenceInputStream(Collections.enumeration(map { it.asInputStream() }.toList()))
