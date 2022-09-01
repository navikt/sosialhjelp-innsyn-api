package no.nav.sosialhjelp.innsyn.idporten

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.oauth2.sdk.AccessTokenResponse
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.RefreshTokenGrant
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenErrorResponse
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.LogoutRequest
import com.nimbusds.openid.connect.sdk.Nonce
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.app.exceptions.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.app.tokendings.createSignedAssertion
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URL
import java.util.UUID

@Component
class IdPortenClient(
    private val idPortenProperties: IdPortenProperties,
    private val redisService: RedisService,
) {

    fun getAuthorizeUrl(sessionId: String, redirectPath: String?): URI {
        redirectPath?.let { redisService.put("LOGIN_REDIRECT_$sessionId", it.toByteArray(), idPortenProperties.loginTimeout) }
        val state = State().also {
            redisService.put("IDPORTEN_STATE_$sessionId", objectMapper.writeValueAsBytes(it), idPortenProperties.loginTimeout)
        }
        val nonce = Nonce().also {
            redisService.put("IDPORTEN_NONCE_$sessionId", objectMapper.writeValueAsBytes(it), idPortenProperties.loginTimeout)
        }

        val codeVerifier = CodeVerifier().also {
            redisService.put("IDPORTEN_CODE_VERIFIER_$sessionId", it.value.toByteArray(), idPortenProperties.loginTimeout)
        }

        return AuthorizationRequest.Builder(
            ResponseType(ResponseType.Value.CODE),
            ClientID(idPortenProperties.clientId)
        )
            .scope(Scope("openid", "profile", "ks:fiks"))
            .state(state)
            .customParameter("nonce", nonce.value)
            .customParameter("acr_values", "Level4")
            .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
            .redirectionURI(URI(idPortenProperties.redirectUri))
            .endpointURI(URI(idPortenProperties.wellKnown.authorizationEndpoint))
            .build()
            .toURI()
    }

    fun getToken(authorizationCode: AuthorizationCode?, codeVerifier: CodeVerifier, sessionId: String) {
        val callback = URI(idPortenProperties.redirectUri)
        val codeGrant: AuthorizationGrant = AuthorizationCodeGrant(authorizationCode, callback, codeVerifier)
        val clientAuth = PrivateKeyJWT(SignedJWT.parse(clientAssertion))

        val tokenRequest = TokenRequest(
            URI(idPortenProperties.wellKnown.tokenEndpoint),
            clientAuth,
            codeGrant,
            Scope("openid", "profile", "ks:fiks")
        )

        val httpResponse: HTTPResponse = tokenRequest.toHTTPRequest().send()
        val tokenResponse = objectMapper.readValue<TokenResponse>(httpResponse.content)

        val jwtProcessor = DefaultJWTProcessor<SecurityContext>()
        val keySource = RemoteJWKSet<SecurityContext>(URL(idPortenProperties.wellKnown.jwksUri))
        val keySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource)

        val storedNonce = redisService.get("IDPORTEN_NONCE_$sessionId", Nonce::class.java) as? Nonce
            ?: throw TilgangskontrollException("No nonce found on sessionId")

        jwtProcessor.jwsKeySelector = keySelector
        jwtProcessor.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
            JWTClaimsSet.Builder()
                .issuer(idPortenProperties.wellKnown.issuer)
                .claim("nonce", storedNonce.value)
                .build(),
            setOf("sid", "nonce")
        )
        val claimsSet = jwtProcessor.process(tokenResponse.idToken, null)

        val sid = claimsSet.getStringClaim("sid")
        if (sid.isEmpty()) {
            log.error("Finner ikke \"sid\" claim i jwt fra idporten. Utlogging vil bli litt trøblete.")
        }

        sid?.let { redisService.put("IDPORTEN_SESSION_ID_$it", sessionId.toByteArray(), idPortenProperties.sessionTimeout) }
        redisService.put("IDPORTEN_ACCESS_TOKEN_$sessionId", tokenResponse.accessToken.toByteArray(), idPortenProperties.tokenTimeout)
        redisService.put("IDPORTEN_REFRESH_TOKEN_$sessionId", tokenResponse.refreshToken.toByteArray(), idPortenProperties.sessionTimeout)
        redisService.put("IDPORTEN_ID_TOKEN_$sessionId", tokenResponse.idToken.toByteArray())
    }

    fun getAccessTokenFromRefreshToken(refreshTokenString: String, loginId: String): String? {
        val refreshToken = RefreshToken(refreshTokenString)
        val refreshTokenGrant: AuthorizationGrant = RefreshTokenGrant(refreshToken)
        val clientAuth = PrivateKeyJWT(SignedJWT.parse(clientAssertion))

        val tokenEndpoint = URI(idPortenProperties.wellKnown.tokenEndpoint)

        // Make the token request
        val request = TokenRequest(tokenEndpoint, clientAuth, refreshTokenGrant)
        val response = com.nimbusds.oauth2.sdk.TokenResponse.parse(request.toHTTPRequest().send())

        if (!response.indicatesSuccess()) {
            // We got an error response...
            val errorResponse: TokenErrorResponse = response.toErrorResponse()
            log.error("Error: ${errorResponse.errorObject}")
            return null
        }

        val successResponse: AccessTokenResponse = response.toSuccessResponse()

        // Get the access token, the refresh token may be updated
        val accessToken: AccessToken = successResponse.tokens.accessToken
        val maybeUpdatedRefreshToken = successResponse.tokens.refreshToken
        successResponse.tokens.bearerAccessToken

        redisService.put("IDPORTEN_ACCESS_TOKEN_$loginId", accessToken.value.toByteArray())
        if (maybeUpdatedRefreshToken.value != refreshTokenString) {
            redisService.put("IDPORTEN_REFRESH_TOKEN_$loginId", maybeUpdatedRefreshToken.value.toByteArray(), 600)
        }

        return accessToken.value
    }

    fun getEndSessionRedirectUrl(loginId: String?): URI {
        if (loginId == null) {
            log.info("Ingen sesjonsId funnet - redirecter til /endsession uten id_token_hint og post_logout_redirect_uri")
            return LogoutRequest(endSessionEndpointURI).toURI()
        }

        val idToken = redisService.get("IDPORTEN_ID_TOKEN_$loginId", String::class.java)
        if (idToken == null) {
            log.info("Fant ikke id_token i cache - redirecter til /endsession uten id_token_hint og post_logout_redirect_uri")
            return LogoutRequest(endSessionEndpointURI).toURI()
        }

        val idTokenString = idToken as String
        val logoutRequest = LogoutRequest(
            endSessionEndpointURI,
            SignedJWT.parse(idTokenString),
            URI(idPortenProperties.postLogoutRedirectUri),
            null // State er optional.
        )
        return logoutRequest.toURI()
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

    private val endSessionEndpointURI get() = URI(idPortenProperties.wellKnown.endSessionEndpoint)

    companion object {
        private val log by logger()
    }
}
