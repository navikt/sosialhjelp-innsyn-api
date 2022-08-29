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

    fun getAuthorizeUrl(sessionId: String): URI {
        val state = State().also {
            redisService.put("IDPORTEN_STATE_$sessionId", objectMapper.writeValueAsBytes(it))
        }
        val nonce = Nonce().also {
            redisService.put("IDPORTEN_NONCE_$sessionId", objectMapper.writeValueAsBytes(it))
        }

        val codeVerifier = CodeVerifier().also {
            redisService.put("IDPORTEN_CODE_VERIFIER_$sessionId", it.value.toByteArray())
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
        jwtProcessor.jwsKeySelector = keySelector
        jwtProcessor.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
            JWTClaimsSet.Builder().issuer(idPortenProperties.wellKnown.issuer).build(),
            setOf("sid")
        )
        val claimsSet = jwtProcessor.process(tokenResponse.idToken, null)

        val sid = claimsSet.getStringClaim("sid")
        if (sid.isEmpty()) throw RuntimeException("Empty sid")

        redisService.put("IDPORTEN_SESSION_ID_$sid", sessionId.toByteArray())
        redisService.put("IDPORTEN_ACCESS_TOKEN_$sessionId", tokenResponse.accessToken.toByteArray())
        redisService.put("IDPORTEN_REFRESH_TOKEN_$sessionId", tokenResponse.refreshToken.toByteArray(), 600)
        redisService.put("IDPORTEN_ID_TOKEN_$sessionId", tokenResponse.idToken.toByteArray())
    }

    fun getAccessTokenFromRefreshToken(refreshTokenString: String, loginId: String): String {
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
            throw RuntimeException("Fikk ikke tak i token")
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

    fun getEndSessionRedirectUrl(loginId: String): URI {
        val endSessionEndpointURI = URI(idPortenProperties.wellKnown.endSessionEndpoint)
        val postLogoutRedirectURI = URI(idPortenProperties.postLogoutRedirectUri)
        val idToken = redisService.get("IDPORTEN_ID_TOKEN_$loginId", String::class.java) ?: RuntimeException("Uh-oh, fant ikke id_token i cache")
        val idTokenString = idToken as String
        val state = State()

        val logoutRequest = LogoutRequest(
            endSessionEndpointURI,
            SignedJWT.parse(idTokenString),
            postLogoutRedirectURI,
            state
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

    companion object {
        private val log by logger()
    }
}
