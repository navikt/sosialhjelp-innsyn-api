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

fun detectTikaType(inputStream: InputStream): TikaFileType {
    val type = Tika().detect(inputStream)
    if(type.equals("application/pdf", ignoreCase = true) ) return TikaFileType.PDF
    if(type.equals("image/png", ignoreCase = true) ) return TikaFileType.PNG
    if(type.equals("image/jpeg", ignoreCase = true) ) return TikaFileType.JPEG

    return TikaFileType.UNKNOWN
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
