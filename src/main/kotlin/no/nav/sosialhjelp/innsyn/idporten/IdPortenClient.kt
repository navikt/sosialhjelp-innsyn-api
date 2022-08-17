package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.openid.connect.sdk.Nonce
import no.nav.sosialhjelp.innsyn.app.tokendings.downloadWellKnown
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class IdPortenClient(
    @Value("\${idporten_well_known_url}") private val idportenWellKnownUrl: String,
    @Value("\${idporten_clientid}") private val idportenClientId: String,
    @Value("\${idporten_redirect_uri}") private val idportenRedirectUri: String,
) {

    val wellknown = downloadWellKnown(idportenWellKnownUrl)

    fun getAuthorizeUrl(): URI {

        // todo: lagre state til redis

        val state = State()
        val nonce = Nonce()

        val codeVerifier = CodeVerifier()

        return AuthorizationRequest.Builder(
            ResponseType(ResponseType.Value.CODE),
            ClientID(idportenClientId)
        )
            .scope(Scope("openid", "profile")) // , "ks:fiks")) // KS må godkjenne vår clientId
            .state(state)
            .customParameter("nonce", nonce.value)
            .customParameter("acr_values", "Level4")
            .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
            .redirectionURI(URI(idportenRedirectUri))
            .endpointURI(URI(wellknown.authorizationEndpoint))
            .build()
            .toURI()
    }
}
