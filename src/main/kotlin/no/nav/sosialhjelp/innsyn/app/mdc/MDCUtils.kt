package no.nav.sosialhjelp.innsyn.app.mdc

import org.slf4j.MDC
import java.security.SecureRandom

object MDCUtils {
    const val CALL_ID = "callId"
    const val DIGISOS_ID = "digisosId"
    const val PATH = "path"
    const val USER_AGENT = "userAgent"
    const val REFERER = "request_Referer"

    private val RANDOM = SecureRandom()

    fun get(key: String): String? = MDC.get(key)

    fun put(
        key: String,
        value: String,
    ) {
        MDC.put(key, value)
    }

    fun clearMDC() {
        MDC.remove(CALL_ID)
        MDC.remove(DIGISOS_ID)
        MDC.remove(PATH)
        MDC.remove(USER_AGENT)
        MDC.remove(REFERER)
    }

    fun generateCallId(): String {
        val randomNr = RANDOM.nextInt(Integer.MAX_VALUE)
        val systemTime = System.currentTimeMillis()

        return "CallId_${systemTime}_$randomNr"
    }
}
