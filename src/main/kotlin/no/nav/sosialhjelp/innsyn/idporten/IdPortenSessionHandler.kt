package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.ACCESS_TOKEN_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.CODE_VERIFIER_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.ID_TOKEN_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.NONCE_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.REFRESH_TOKEN_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.SESSION_ID_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.idporten.CachePrefixes.STATE_CACHE_PREFIX
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Profile("dev")
@Component
class IdPortenSessionHandler(
    private val redisService: RedisService,
    private val idPortenClient: IdPortenClient
) {

    fun clearSession(sid: String) {
        val loginId = redisService.get("$SESSION_ID_CACHE_PREFIX$sid", String::class.java)
        if (loginId != null) {
            clearTokens(loginId)
            redisService.delete("$SESSION_ID_CACHE_PREFIX$sid")
        }
    }

    fun clearTokens(loginId: String) {
        redisService.delete("$REFRESH_TOKEN_CACHE_PREFIX$loginId")
        redisService.delete("$ACCESS_TOKEN_CACHE_PREFIX$loginId")
        redisService.delete("$ID_TOKEN_CACHE_PREFIX$loginId")
    }

    fun clearPropertiesForLogin(loginId: String) {
        redisService.delete("$STATE_CACHE_PREFIX$loginId")
        redisService.delete("$NONCE_CACHE_PREFIX$loginId")
        redisService.delete("$CODE_VERIFIER_CACHE_PREFIX$loginId")
    }

    fun getToken(request: HttpServletRequest): String? {
        val loginId = request.cookies?.firstOrNull { it.name == "login_id" }?.value
        if (loginId != null) {
            val accessToken = redisService.get("$ACCESS_TOKEN_CACHE_PREFIX$loginId", String::class.java)
            return accessToken ?: getAccessTokenFromRefreshToken(loginId)
        }
        return null
    }

    private fun getAccessTokenFromRefreshToken(loginId: String): String? {
        val refreshToken = redisService.get("$REFRESH_TOKEN_CACHE_PREFIX$loginId", String::class.java)
        return refreshToken?.let { idPortenClient.getAccessTokenFromRefreshToken(it, loginId) }
    }
}
