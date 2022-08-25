package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.oauth2.sdk.AuthorizationResponse
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import no.nav.security.token.support.core.api.Unprotected
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.app.tokendings.createSignedAssertion
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
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
    ): ResponseEntity<Nothing> {

        val sessionId = UUID.randomUUID().toString()

        val redirectLocation = idPortenClient.getAuthorizeUrl(sessionId).toString()

        redirectPath?.let { redisService.put("LOGIN_REDIRECT_$sessionId", it.toByteArray()) }

        return nonCacheableRedirectResponse(redirectLocation, sessionId)
    }

    @Unprotected
    @GetMapping("/oauth2/callback") // samme som 'redirectPath' i nais.yaml
    fun handleCallback(request: HttpServletRequest): ResponseEntity<String> {
        val redirectUri = request.requestURL.append('?').append(request.queryString).toString()
        val response = AuthorizationResponse.parse(URI(redirectUri))

        val sessionId = request.cookies.firstOrNull { it.name == "login_id" }?.value
            ?: throw RuntimeException("No sessionId found on cookie")
        val state = redisService.get("IDPORTEN_STATE_$sessionId", State::class.java) as? State
            ?: throw RuntimeException("No state found on sessionId")

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

        redisService.put("IDPORTEN_CODE_$sessionId", objectMapper.writeValueAsBytes(code))

        // for å teste /token-endepunkt lokalt
        log.info("code: ${code.value}")
        log.info("client_assertion: $clientAssertion")

        val codeVerifierValue = redisService.get("IDPORTEN_CODE_VERIFIER_$sessionId", String::class.java) as? String
            ?: throw RuntimeException("No code_verifier found on sessionId")

        idPortenClient.getToken(code, clientAssertion, CodeVerifier(codeVerifierValue), sessionId)

        val headers = HttpHeaders()
        val redirect = redisService.get("LOGIN_REDIRECT_$sessionId", String::class.java) as String?
        headers.set(HttpHeaders.LOCATION, redirect ?: "/sosialhjelp/innsyn")
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build()
    }

    private val clientAssertion get() = createSignedAssertion(
        clientId = idPortenProperties.clientId,
        audience = idPortenProperties.wellKnown.issuer,
        rsaKey = privateRsaKey
    )

    private val privateRsaKey: RSAKey = if (idPortenProperties.clientJwk == "generateRSA") {
        if (MiljoUtils.isRunningInProd()) throw RuntimeException("Generation of RSA keys is not allowed in prod.")
        RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).keyID(UUID.randomUUID().toString()).generate()
    } else {
        RSAKey.parse(idPortenProperties.clientJwk)
    }

    companion object {
        private val log by logger()

        private fun nonCacheableResponse(status: HttpStatus): ResponseEntity.BodyBuilder {
            val headers = HttpHeaders()
            headers.add(HttpHeaders.CACHE_CONTROL, "no-store, no-cache")
            headers.add(HttpHeaders.PRAGMA, "no-cache")
            return ResponseEntity.status(status).headers(headers)
        }

        private fun nonCacheableRedirectResponse(redirectLocation: String, loginId: String): ResponseEntity<Nothing> {
            return nonCacheableResponse(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectLocation)
                .header(HttpHeaders.SET_COOKIE, "login_id=$loginId; Max-Age=3600; Path=/; Secure; HttpOnly")
                .build()
        }
    }
}
