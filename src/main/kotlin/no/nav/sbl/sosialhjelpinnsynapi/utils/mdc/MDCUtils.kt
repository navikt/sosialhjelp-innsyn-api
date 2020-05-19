package no.nav.sbl.sosialhjelpinnsynapi.utils.mdc

import org.slf4j.MDC
import java.security.SecureRandom

object MDCUtils {

    private const val CALL_ID = "callId"
    private const val DIGISOS_ID = "digisosId"

    private val RANDOM = SecureRandom()

    fun getCallId(): String? {
        return MDC.get(CALL_ID)
    }

    fun setCallId(callId: String) {
        MDC.put(CALL_ID, callId)
    }

    fun getDigisosId(): String? {
        return MDC.get(DIGISOS_ID)
    }

    fun setDigisosId(digisosId: String) {
        MDC.put(DIGISOS_ID, digisosId)
    }

    fun clearMDC() {
        MDC.remove(CALL_ID)
        MDC.remove(DIGISOS_ID)
    }

    fun generateCallId(): String {
        val randomNr = RANDOM.nextInt(Integer.MAX_VALUE)
        val systemTime = System.currentTimeMillis()

        return "CallId_${systemTime}_${randomNr}"
    }

}