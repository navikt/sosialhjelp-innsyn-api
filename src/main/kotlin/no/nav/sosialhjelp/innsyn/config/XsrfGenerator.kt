package no.nav.sosialhjelp.innsyn.config

import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.springframework.web.context.request.RequestContextHolder
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

    @JvmOverloads
    fun generateXsrfToken(token: String, date: String = DateTime().toString("yyyyMMdd")): String {
        try {
            val sessionId = RequestContextHolder.currentRequestAttributes().sessionId
            val signKey = token + sessionId + date
            val hmac = Mac.getInstance("HmacSHA256")
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

        val token = generateXsrfToken(idportenIdtoken)
        val valid = token == givenToken || generateXsrfToken(DateTime().minusDays(1).toString("yyyyMMdd")) == givenToken
        require(valid) { "Feil token" }
    }
}
