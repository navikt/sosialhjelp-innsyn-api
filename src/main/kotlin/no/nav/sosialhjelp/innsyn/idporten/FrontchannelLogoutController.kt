package no.nav.sosialhjelp.innsyn.idporten

import no.nav.security.token.support.core.api.Unprotected
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FrontchannelLogoutController(
    private val idPortenProperties: IdPortenProperties,
    private val idPortenSessionHandler: IdPortenSessionHandler,
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

        if (idPortenSessionId == null || !isValidSid(idPortenSessionId)) {
            log.warn("Ugyldig 'sid' ved utlogging")
            return ResponseEntity.badRequest().build()
        }

        log.info("Frontchannel logout trigget")
        idPortenSessionHandler.clearSession(idPortenSessionId)
        return ResponseEntity.ok().build()
    }

    private fun isValidSid(sid: String): Boolean {
        return sid.matches(Regex("[0-9a-zA-Z_=-]{1,100}"))
    }

    companion object {
        private val log by logger()
    }
}
