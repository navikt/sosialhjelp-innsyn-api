package no.nav.sbl.sosialhjelpinnsynapi.utils

import org.apache.commons.lang3.ArrayUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.tika.Tika
import java.io.IOException
import java.security.MessageDigest

fun getSha512FromByteArray(bytes: ByteArray?): String {
    if (bytes == null) {
        return ""
    }

    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

fun isPng(bytes: ByteArray): Boolean {
    return Tika().detect(ArrayUtils.subarray(bytes.clone(), 0, 2048)).equals("image/png", ignoreCase = true)
}

fun isPdf(bytes: ByteArray): Boolean {
    return Tika().detect(bytes).equals("application/pdf", ignoreCase = true)
}

fun isJpg(bytes: ByteArray): Boolean {
    return Tika().detect(bytes).equals("image/jpeg", ignoreCase = true)
}

fun isImage(bytes: ByteArray): Boolean {
    return isJpg(bytes) || isPng(bytes)
}

fun pdfIsSigned(pdf: PDDocument): Boolean {
    try {
        return pdf.signatureDictionaries.isNotEmpty()
    } catch (var3: IOException) {
        throw RuntimeException("Kunne ikke lese siganturinformasjon fra PDF")
    }

}