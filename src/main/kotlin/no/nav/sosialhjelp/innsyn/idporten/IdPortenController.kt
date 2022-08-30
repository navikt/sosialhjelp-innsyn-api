package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.oauth2.sdk.AuthorizationResponse
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import no.nav.security.token.support.core.api.Unprotected
import no.nav.sosialhjelp.innsyn.app.exceptions.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@RestController
class IdPortenController(
    private val idPortenClient: IdPortenClient,
    private val idPortenProperties: IdPortenProperties,
    private val redisService: RedisService,
) {

    @Unprotected
    @GetMapping("/oauth2/login")
    fun login(
        request: HttpServletRequest,
        @RequestParam("goto") redirectPath: String?,
    ): ResponseEntity<String> {

        val sessionId = UUID.randomUUID().toString()

        val redirectLocation = idPortenClient.getAuthorizeUrl(sessionId, redirectPath).toString()

        return nonCacheableRedirectResponse(redirectLocation, sessionId)
    }

    @Unprotected
    @GetMapping("/oauth2/callback") // samme som 'redirectPath' i nais.yaml
    fun handleCallback(request: HttpServletRequest): ResponseEntity<String> {
        val redirectUri = request.requestURL.append('?').append(request.queryString).toString()
        val response = AuthorizationResponse.parse(URI(redirectUri))

        val sessionId = request.cookies.firstOrNull { it.name == "login_id" }?.value
            ?: throw TilgangskontrollException("No login_id found from cookie")
        val state = redisService.get("IDPORTEN_STATE_$sessionId", State::class.java) as? State
            ?: throw TilgangskontrollException("No state found on sessionId")

        // Check the returned state parameter, must match the original
        if (state != response.state) {
            // Unexpected or tampered response, stop!!!
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (!response.indicatesSuccess()) {
            // The request was denied or some error occurred
            val errorResponse = response.toErrorResponse()
            log.error("Error: ${errorResponse.errorObject}")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val successResponse = response.toSuccessResponse()

        // Retrieve the authorisation code, to be used later to exchange the code for
        // an access token at the token endpoint of the server
        val code = successResponse.authorizationCode

        val codeVerifierValue = redisService.get("IDPORTEN_CODE_VERIFIER_$sessionId", String::class.java) as? String
            ?: throw TilgangskontrollException("No code_verifier found on sessionId")

        idPortenClient.getToken(code, CodeVerifier(codeVerifierValue), sessionId)

        // Disse trengs bare midlertidig
        redisService.delete("IDPORTEN_STATE_$sessionId")
        redisService.delete("IDPORTEN_NONCE_$sessionId")
        redisService.delete("IDPORTEN_CODE_VERIFIER_$sessionId")

        val redirect = redisService.get("LOGIN_REDIRECT_$sessionId", String::class.java) as String?
        return nonCacheableRedirectResponse(redirect ?: "/sosialhjelp/innsyn")
    }

    /**
     * Utlogging initiert fra egen tjeneste (/endsession)
     */
    @Unprotected
    @GetMapping("/oauth2/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<String> {
        val loginId = request.cookies.firstOrNull { it.name == "login_id" }?.value

        val endSessionRedirectUrl = idPortenClient.getEndSessionRedirectUrl(loginId)

        log.debug("Single-logout fra egen tjeneste")
        redisService.delete("IDPORTEN_REFRESH_TOKEN_$loginId")
        redisService.delete("IDPORTEN_ACCESS_TOKEN_$loginId")
        redisService.delete("IDPORTEN_ID_TOKEN_$loginId")

        return nonCacheableRedirectResponse(endSessionRedirectUrl.toString())
    }

    private fun nonCacheableRedirectResponse(redirectLocation: String, loginId: String? = null): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.CACHE_CONTROL, "no-store, no-cache")
        headers.add(HttpHeaders.PRAGMA, "no-cache")
        headers.add(HttpHeaders.LOCATION, redirectLocation)
        loginId?.let { headers.add(HttpHeaders.SET_COOKIE, "login_id=$it; Max-Age=${idPortenProperties.sessionTimeout}; Path=/; Secure; HttpOnly") }
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build()
    }

    companion object {
        private val log by logger()
    }
}
