package no.nav.sosialhjelp.innsyn.app.tokendings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.awaitBody

@Component
internal class TokendingsClient(
    private val tokendingsWebClient: TokendingsWebClient,
) {
    suspend fun exchangeToken(
        subjectToken: String,
        clientAssertion: String,
        audience: String,
    ): TokendingsResponse {
        return withContext(Dispatchers.IO) {
            val params = LinkedMultiValueMap<String, String>()
            params.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
            params.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
            params.add("client_assertion", clientAssertion)
            params.add("subject_token_type", "urn:ietf:params:oauth:token-type:jwt")
            params.add("subject_token", subjectToken)
            params.add("audience", audience)

            tokendingsWebClient.webClient.post()
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .awaitBody()
        }
    }
}
