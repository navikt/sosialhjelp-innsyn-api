package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.common.XsrfException
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.redis.XSRF_KEY_PREFIX
import no.nav.sosialhjelp.innsyn.utils.sha256
import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Component
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest

/**
 * Klasse som genererer og sjekker xsrf token som sendes inn
 */
@Component
class XsrfGenerator(
    private val redisService: RedisService,
) {
    private val SECRET = System.getenv("XSRF_SECRET") ?: "hemmelig"

    fun generateXsrfToken(date: LocalDateTime = LocalDateTime.now()): String {
        val fnr = SubjectHandlerUtils.getUserIdFromToken()
        redisService.get(redisKey(fnr, date), String::class.java)?.let { return it as String }
        try {
            val xsrf = fnr + UUID.randomUUID().toString()
            val hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(SECRET.toByteArray(), "HmacSHA256")
            hmac.init(secretKey)
            return Base64.encodeBase64URLSafeString(hmac.doFinal(xsrf.toByteArray()))
                .also { lagreTilRedis(redisKey(fnr, date), it) }
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException("Kunne ikke generere token: ", e)
        }
    }

    private fun hentXsrfToken(id: String, date: LocalDateTime = LocalDateTime.now()): String? {
        return hentFraRedis(redisKey(id, date))
    }

    private fun lagreTilRedis(redisKey: String, xsrfString: String) {
        redisService.put(XSRF_KEY_PREFIX + redisKey, xsrfString.toByteArray(), 8 * 60L * 60L)
    }

    private fun hentFraRedis(redisKey: String): String? {
        return redisService.get(XSRF_KEY_PREFIX + redisKey, String::class.java) as String?
    }

    fun sjekkXsrfToken(request: HttpServletRequest) {
        val fnr = SubjectHandlerUtils.getUserIdFromToken()
        val xsrfRequestString = request.getHeader("XSRF-TOKEN-INNSYN-API")

        val yesterday = LocalDateTime.now().minusDays(1)

        val xsrfToken = hentXsrfToken(fnr) ?: UUID.randomUUID().toString()
        val yesterdaysXsrfToken = hentXsrfToken(fnr, yesterday) ?: UUID.randomUUID().toString()

        val valid = xsrfToken == xsrfRequestString || yesterdaysXsrfToken == xsrfRequestString
        if (!valid) throw XsrfException("Feil xsrf token")
    }

    companion object {
        private val redisDatoFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        fun redisKey(token: String, date: LocalDateTime) = sha256(token + redisDatoFormatter.format(date))
    }
}
