package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.openid.connect.sdk.Nonce
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import java.net.URI

@Component
class IdPortenClient(
    private val idPortenProperties: IdPortenProperties,
    private val redisService: RedisService,
) {

    fun getAuthorizeUrl(): URI {
        val sessionId = RequestContextHolder.currentRequestAttributes().sessionId
        val state = State().also {
            redisService.put("IDPORTEN_STATE_$sessionId", objectMapper.writeValueAsBytes(it))
        }
        val nonce = Nonce().also {
            redisService.put("IDPORTEN_NONCE_$sessionId", objectMapper.writeValueAsBytes(it))
        }

        val codeVerifier = CodeVerifier().also {
            redisService.put("IDPORTEN_CODE_VERIFIER_$sessionId", it.value.toByteArray())
        }
        log.info("code_verifier: ${codeVerifier.value}")

        return AuthorizationRequest.Builder(
            ResponseType(ResponseType.Value.CODE),
            ClientID(idPortenProperties.clientId)
        )
            .scope(Scope("openid", "profile")) // , "ks:fiks")) // Hvis ks:fiks scope skal med her?
            .state(state)
            .customParameter("nonce", nonce.value)
            .customParameter("acr_values", "Level4")
            .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
            .redirectionURI(URI(idPortenProperties.redirectUri))
            .endpointURI(URI(idPortenProperties.wellKnown.authorizationEndpoint))
            .build()
            .toURI()
    }

    fun getToken(authorizationCode: AuthorizationCode?, clientAssertion: String, codeVerifier: CodeVerifier) {
        val callback = URI(idPortenProperties.redirectUri)
        val codeGrant: AuthorizationGrant = AuthorizationCodeGrant(authorizationCode, callback, codeVerifier)
        val clientAuth = PrivateKeyJWT(SignedJWT.parse(clientAssertion))

        val tokenRequest = TokenRequest(
            URI(idPortenProperties.wellKnown.tokenEndpoint),
            clientAuth,
            codeGrant,
            Scope("openid profile ks:fiks")
        )

        val httpResponse: HTTPResponse = tokenRequest.toHTTPRequest().send()

        log.info("Response: ${httpResponse.content}")
    }

    companion object {
        private val log by logger()
    }
}
