package no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc

import org.slf4j.MDC

object MDCUtils {
    const val DIGISOS_ID = "digisosId"
    const val PATH = "path"
    const val USER_AGENT = "userAgent"
    const val REFERER = "request_Referer"

    fun get(key: String): String? = MDC.get(key)

    fun put(
        key: String,
        value: String,
    ) {
        MDC.put(key, value)
    }

    fun clearMDC() {
        MDC.clear()
    }
}
