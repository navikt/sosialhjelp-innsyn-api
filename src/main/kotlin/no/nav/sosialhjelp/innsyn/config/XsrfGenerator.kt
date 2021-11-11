package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.logger
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest

/**
 * Klasse som genererer og sjekker xsrf token som sendes inn
 */
object XsrfGenerator {
    private val SECRET = System.getenv("XSRF_SECRET") ?: "hemmelig"
    private val xsrf = Xsrf()

    @JvmOverloads
    fun generateXsrfToken(token: String, sessionId: String, date: String = DateTime().toString("yyyyMMdd")): String {
        return xsrf.generateXsrfToken(token, sessionId, date)
    }

    fun sjekkXsrfToken(request: HttpServletRequest) {
        xsrf.sjekkXsrfToken(request)
    }

    class Xsrf {
        fun generateXsrfToken(token: String, sessionId: String, date: String = DateTime().toString("yyyyMMdd")): String {
            try {
                val signKey = token + sessionId + date
                val hmac = Mac.getInstance("HmacSHA256")
                log.info("Generate: $sessionId")
                val secretKey = SecretKeySpec(SECRET.toByteArray(), "HmacSHA256")
                hmac.init(secretKey)
                return Base64.encodeBase64URLSafeString(hmac.doFinal(signKey.toByteArray()))
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalArgumentException("Kunne ikke generere token: ", e)
            }
        }

        fun sjekkXsrfToken(request: HttpServletRequest) {
            val cookies = request.cookies ?: emptyArray()
            val idportenTokenOptional = Arrays.stream(cookies).filter { c -> c.name == "idporten-idtoken" }.findFirst()
            var idportenIdtoken = "default"
            if (idportenTokenOptional.isPresent) {
                idportenIdtoken = idportenTokenOptional.get().value
            }

            val givenTokenOptional = Arrays.stream(cookies).filter { c -> c.name == "XSRF-TOKEN-INNSYN-API" }.findFirst()
            var givenToken = "default"
            if (givenTokenOptional.isPresent) {
                givenToken = givenTokenOptional.get().value
            }

            val cookieSessionId = request.cookies.firstOrNull { it.name == "sosialhjelp-innsyn-id" }?.value
            require(cookieSessionId != null) { "Mangler session id cookie" }

            val xsrfToken = generateXsrfToken(idportenIdtoken, cookieSessionId)
            val yesterday = DateTime().minusDays(1).toString("yyyyMMdd")
            val yesterdaysXsrfToken = generateXsrfToken(idportenIdtoken, cookieSessionId, yesterday)
            val valid = xsrfToken == givenToken || yesterdaysXsrfToken == givenToken
            require(valid) { "Feil xsrf token" }
        }
        companion object {
            val log by logger()
        }
    }
}
