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
            log.error("Feil issuer ved utlogging")
            return ResponseEntity.badRequest().build()
        }

        if (!isValidSid(idPortenSessionId)) {
            log.error("Ugyldig 'sid' ved utlogging")
            return ResponseEntity.badRequest().build()
        }

        val sessionId = redisService.get("IDPORTEN_SESSION_ID_$idPortenSessionId", String::class.java) as? String
        if (sessionId != null) {
            log.debug("Frontchannel logout")
            clearCache(sessionId)
            redisService.delete("IDPORTEN_SESSION_ID_$idPortenSessionId")
            // clear security context ?
            return ResponseEntity.ok().build()
        }

        return ResponseEntity.badRequest().build()
    }

    private fun isValidSid(sid: String?): Boolean {
        if (sid == null) return false
        return sid.matches(Regex("[0-9a-zA-Z_=-]{1,100}"))
    }

    private fun clearCache(sessionId: String) {
        redisService.delete("IDPORTEN_STATE_$sessionId")
        redisService.delete("IDPORTEN_NONCE_$sessionId")
        redisService.delete("IDPORTEN_CODE_VERIFIER_$sessionId")
        redisService.delete("IDPORTEN_CODE_$sessionId")
        redisService.delete("IDPORTEN_REFRESH_TOKEN_$sessionId")
    }

    companion object {
        private val log by logger()
    }
}
