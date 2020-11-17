package no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg

import org.apache.tika.Tika
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

fun isPdf(inputStream: InputStream): Boolean {
    return Tika().detect(inputStream).equals("application/pdf", ignoreCase = true)
}

fun isImage(inputStream: InputStream): Boolean {
    val type1 = Tika().detect(inputStream.readAllBytes())
    log.info("VedleggUtils.isImage bytearray - mimetype={}", type1)
    val type = Tika().detect(inputStream)
    log.info("VedleggUtils.isImage inputstream - mimetype={}", type)
    return type == "image/png" || type == "image/jpeg"
}

private val log: Logger = LoggerFactory.getLogger(VedleggService::class.java)
