package no.nav.sosialhjelp.innsyn.idporten

import no.nav.security.token.support.core.api.Unprotected
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FrontchannelLogoutController(
    private val idPortenProperties: IdPortenProperties,
    private val redisService: RedisService,
) {

    /**
     * Front-channel logouts are logouts initiated by other ID-porten clients
     */
    @Unprotected
    @GetMapping("/frontchannel/logout")
    fun frontchannelLogout(
        @RequestParam(name = "iss") issuer: String?,
        @RequestParam(name = "sid") idPortenSessionId: String?,
    ): ResponseEntity<String> {
        if (idPortenProperties.wellKnown.issuer != issuer) {
            log.warn("Feil issuer ved utlogging")
            return ResponseEntity.badRequest().build()
        }

        if (!isValidSid(idPortenSessionId)) {
            log.warn("Ugyldig 'sid' ved utlogging")
            return ResponseEntity.badRequest().build()
        }

        val sessionId = redisService.get("IDPORTEN_SESSION_ID_$idPortenSessionId", String::class.java) as? String
        if (sessionId != null) {
            log.debug("Frontchannel logout")
            redisService.delete("IDPORTEN_REFRESH_TOKEN_$sessionId")
            redisService.delete("IDPORTEN_ACCESS_TOKEN_$sessionId")
            redisService.delete("IDPORTEN_ID_TOKEN_$sessionId")
            redisService.delete("IDPORTEN_SESSION_ID_$idPortenSessionId")
            return ResponseEntity.ok().build()
        }

        return ResponseEntity.badRequest().build()
    }

    private fun isValidSid(sid: String?): Boolean {
        if (sid == null) return false
        return sid.matches(Regex("[0-9a-zA-Z_=-]{1,100}"))
    }

    companion object {
        private val log by logger()
    }
}
