package no.nav.sosialhjelp.innsyn.vedlegg.convert

import org.apache.tika.Tika

object FileDetectionUtils {
    fun detectMimeType(bytes: ByteArray?): String {
        val mimeType = Tika().detect(bytes).lowercase()
        return if (mimeType == MimeTypes.TEXT_X_MATLAB) MimeTypes.APPLICATION_PDF else mimeType
    }
}

object MimeTypes {
    const val APPLICATION_PDF = "application/pdf"
    const val APPLICATION_JSON = "application/json"
    const val TEXT_X_MATLAB = "text/x-matlab"
}
