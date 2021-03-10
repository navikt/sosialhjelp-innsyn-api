package no.nav.sosialhjelp.innsyn.service.vedlegg

import org.apache.tika.Tika
import java.io.InputStream
import java.security.MessageDigest

fun getSha512FromByteArray(bytes: ByteArray?): String {
    if (bytes == null) {
        return ""
    }

    val md = MessageDigest.getInstance("SHA-512")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

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

fun splitFileName(fineName: String): FileNameSplit {
    val returnValue = FileNameSplit(fineName, "")
    val indexOfFileExtention = fineName.lastIndexOf(".")
    if (indexOfFileExtention != -1) {
        returnValue.name = fineName.substring(0, indexOfFileExtention)
        returnValue.extention = fineName.substring(indexOfFileExtention, fineName.length)
    }
    return returnValue
}

class FileNameSplit(var name: String, var extention: String)
