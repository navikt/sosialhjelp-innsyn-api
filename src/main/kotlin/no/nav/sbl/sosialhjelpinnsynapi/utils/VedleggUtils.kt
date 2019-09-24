package no.nav.sbl.sosialhjelpinnsynapi.utils

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.tika.Tika
import java.io.IOException
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

fun isPng(inputStream: InputStream): Boolean {
    return Tika().detect(inputStream).equals("image/png", ignoreCase = true)
}

fun isPdf(inputStream: InputStream): Boolean {
    return Tika().detect(inputStream).equals("application/pdf", ignoreCase = true)
}

fun isJpg(inputStream: InputStream): Boolean {
    return Tika().detect(inputStream).equals("image/jpeg", ignoreCase = true)
}

fun isImage(inputStream: InputStream): Boolean {
    return isJpg(inputStream) || isPng(inputStream)
}

fun pdfIsSigned(pdf: PDDocument): Boolean {
    try {
        return pdf.signatureDictionaries.isNotEmpty()
    } catch (var3: IOException) {
        throw RuntimeException("Kunne ikke lese siganturinformasjon fra PDF")
    }

}