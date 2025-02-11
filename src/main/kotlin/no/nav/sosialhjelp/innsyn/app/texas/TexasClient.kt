package no.nav.sosialhjelp.innsyn.app.texas

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody

enum class TokenEndpointType {
    // For kall uten sluttbrukers kontekst
    M2M,

    // For kall med sluttbrukers kontekst
    BEHALF_OF,

    // For å sjekke et token
    INTROSPECTION,
}

@Component
class TexasClient(
    texasWebClientBuilder: WebClient.Builder,
    @Value("\${nais.token.endpoint}")
    private val tokenEndpoint: String,
    @Value("\${nais.token.exchange.endpoint}")
    private val tokenXEndpoint: String,
) {
    private val log by logger()

    private val texasWebClient =
        texasWebClientBuilder.defaultHeaders {
            it.contentType = MediaType.APPLICATION_JSON
        }.codecs {
            it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
            it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
            it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
        }.build()

    private val maskinportenParams: Map<String, String> = mapOf("identity_provider" to "maskinporten", "target" to "ks:fiks")

    private fun getTokenXParams(
        target: String,
        userToken: String,
    ): Map<String, String> = mapOf("identity_provider" to "tokenx", "target" to target, "user_token" to userToken)

    suspend fun getMaskinportenToken() = getToken(TokenEndpointType.M2M, maskinportenParams)

    suspend fun getTokenXToken(
        target: String,
        userToken: String,
    ) = getToken(
        TokenEndpointType.BEHALF_OF,
        getTokenXParams(target, userToken),
    )

    private suspend fun getToken(
        tokenEndpointType: TokenEndpointType,
        params: Map<String, String>,
    ): String =
        withContext(Dispatchers.IO + MDCContext()) {
            val url =
                when (tokenEndpointType) {
                    TokenEndpointType.M2M -> tokenEndpoint
                    TokenEndpointType.BEHALF_OF -> tokenXEndpoint
                    TokenEndpointType.INTROSPECTION -> TODO()
                }
            val response =
                try {
                    texasWebClient.post().uri(url)
                        .bodyValue(params)
                        .retrieve()
                        .awaitBody<TokenResponse.Success>()
                        .also {
                            log.info("Hentet $tokenEndpointType-token fra Texas")
                        }
                } catch (e: WebClientResponseException) {
                    val error =
                        e.getResponseBodyAs(TokenErrorResponse::class.java) ?: TokenErrorResponse(
                            "Unknown error: ${e.responseBodyAsString}",
                            e.message ?: "No message",
                        )

                    TokenResponse.Error(error, e.statusCode)
                }

            when (response) {
                is TokenResponse.Success -> response.accessToken
                is TokenResponse.Error -> {
                    error(
                        "Feil ved henting av $tokenEndpointType-token fra Texas. Statuscode: ${response.status}. Error: ${response.error}",
                    )
                }
            }
        }
}

sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Int,
        @JsonProperty("token_type")
        val tokenType: String,
    ) : TokenResponse()

    data class Error(
        val error: TokenErrorResponse,
        val status: HttpStatusCode,
    ) : TokenResponse()
}

data class TokenErrorResponse(
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String,
)
