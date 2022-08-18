package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.openid.connect.sdk.Nonce
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component
import java.net.URI

@Component
class IdPortenClient(
    private val idPortenProperties: IdPortenProperties
) {

    fun getAuthorizeUrl(): URI {
        // todo: lagre state til redis

        val state = State()
        val nonce = Nonce()

        val codeVerifier = CodeVerifier()
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

    companion object {
        private val log by logger()
    }
}
