package no.nav.sosialhjelp.innsyn.service.vedlegg

import org.apache.tika.Tika
import java.io.InputStream
import java.security.MessageDigest
import java.text.Normalizer

fun getSha512FromByteArray(bytes: ByteArray?): String {
    if (bytes == null) {
        return ""
    }

    val md = MessageDigest.getInstance("SHA-512")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

fun sanitizeFileName(filename: String) = Normalizer.normalize(filename, Normalizer.Form.NFC).trim()

fun detectTikaType(inputStream: InputStream): String {
    return Tika().detect(inputStream)
}

fun mapToTikaFileType(tikaMediaType: String): TikaFileType {
    return when {
        tikaMediaType.equals("application/pdf", ignoreCase = true) -> TikaFileType.PDF
        tikaMediaType.equals("image/png", ignoreCase = true) -> TikaFileType.PNG
        tikaMediaType.equals("image/jpeg", ignoreCase = true) -> TikaFileType.JPEG
        else -> TikaFileType.UNKNOWN
    }
}

enum class TikaFileType {
    JPEG,
    PNG,
    PDF,
    UNKNOWN
}

fun splitFileName(fileName: String): FileNameSplit {
    val returnValue = FileNameSplit(fileName, "")
    val indexOfFileExtension = fileName.lastIndexOf(".")
    if (indexOfFileExtension != -1) {
        val ext = fileName.substring(indexOfFileExtension, fileName.length)
        if (ext in listOf(".jpg", ".jpeg", ".png", ".pdf")) {
            returnValue.name = fileName.substring(0, indexOfFileExtension)
            returnValue.extension = ext
        }
    }
    return returnValue
}

class FileNameSplit(var name: String, var extension: String)
