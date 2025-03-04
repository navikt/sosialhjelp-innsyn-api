package no.nav.sosialhjelp.innsyn.vedlegg.convert

import org.apache.tika.Tika
import org.slf4j.LoggerFactory

object FileDetectionUtils {
    private val log = LoggerFactory.getLogger(FileDetectionUtils::class.java)

    fun detectMimeType(bytes: ByteArray?): String {
        val mimeType = Tika().detect(bytes).lowercase()
        return if (mimeType == MimeTypes.TEXT_X_MATLAB) MimeTypes.APPLICATION_PDF else mimeType
    }
}

object MimeTypes {
    const val APPLICATION_PDF = "application/pdf"
    const val APPLICATION_JSON = "application/json"
    const val IMAGE_PNG = "image/png"
    const val IMAGE_JPEG = "image/jpeg"
    const val TEXT_X_MATLAB = "text/x-matlab"
}

enum class TikaFileType(val extension: String) {
    JPEG(".jpg"),
    PNG(".png"),
    PDF(".pdf"),
    UNKNOWN(""),
}
