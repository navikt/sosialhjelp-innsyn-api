package no.nav.sosialhjelp.innsyn.vedlegg

import org.apache.tika.Tika
import java.io.InputStream
import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

fun getSha512FromByteArray(bytes: ByteArray?): String {
    if (bytes == null) {
        return ""
    }

    val md = MessageDigest.getInstance("SHA-512")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

fun sanitizeFileName(filename: String) = Normalizer.normalize(filename, Normalizer.Form.NFC).trim()

fun detectTikaType(inputStream: InputStream): String = Tika().detect(inputStream)

fun mapToTikaFileType(tikaMediaType: String): TikaFileType =
    when {
        tikaMediaType.equals("application/pdf", ignoreCase = true) -> TikaFileType.PDF
        tikaMediaType.equals("text/x-matlab", ignoreCase = true) -> TikaFileType.PDF
        tikaMediaType.equals("image/png", ignoreCase = true) -> TikaFileType.PNG
        tikaMediaType.equals("image/jpeg", ignoreCase = true) -> TikaFileType.JPEG
        else -> TikaFileType.UNKNOWN
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

class FileNameSplit(
    val name: String,
    val extension: String,
)

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
): Boolean =
    (firstDate == null && secondDate == null) ||
        ChronoUnit.MINUTES.between(firstDate, secondDate).absoluteValue < 1
