package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class IdPortenSessionHandler(
    private val redisService: RedisService,
    private val idPortenClient: IdPortenClient
) {

    fun getToken(request: HttpServletRequest): String? {
        val loginId = request.cookies?.firstOrNull { it.name == "login_id" }?.value
        if (loginId != null) {
            val accessToken = redisService.get("IDPORTEN_ACCESS_TOKEN_$loginId", String::class.java) as String?
            return accessToken ?: getAccessTokenFromRefreshToken(loginId)
        }
        return null
    }

    private fun getAccessTokenFromRefreshToken(loginId: String): String? {
        val refreshToken = redisService.get("IDPORTEN_REFRESH_TOKEN_$loginId", String::class.java) as String?
        return refreshToken?.let { idPortenClient.getAccessTokenFromRefreshToken(it, loginId) }
    }
}
