package no.nav.sosialhjelp.innsyn.app.texas

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.utils.configureCodecs
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
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

sealed class TexasClient(
    texasWebClientBuilder: WebClient.Builder,
    private val tokenEndpoint: String,
    private val tokenXEndpoint: String,
) {
    protected val log by logger()

    open suspend fun getMaskinportenToken(): Token = getToken(TokenEndpointType.M2M, maskinportenParams)

    suspend fun getTokenXToken(
        target: String,
        userToken: Token,
    ): Token =
        getToken(
            TokenEndpointType.BEHALF_OF,
            getTokenXParams(target, userToken),
        )

    private val texasWebClient =
        texasWebClientBuilder
            .defaultHeaders { it.contentType = MediaType.APPLICATION_JSON }
            .configureCodecs()
            .build()

    private val maskinportenParams: Map<String, String> = mapOf("identity_provider" to "maskinporten", "target" to "ks:fiks")

    private fun getTokenXParams(
        target: String,
        userToken: Token,
    ): Map<String, String> = mapOf("identity_provider" to "tokenx", "target" to target, "user_token" to userToken.value)

    protected suspend fun getToken(
        tokenEndpointType: TokenEndpointType,
        params: Map<String, String>,
    ): Token =
        withContext(Dispatchers.IO) {
            val url =
                when (tokenEndpointType) {
                    TokenEndpointType.M2M -> tokenEndpoint
                    TokenEndpointType.BEHALF_OF -> tokenXEndpoint
                    TokenEndpointType.INTROSPECTION -> error("Cannot get token for introspection. Use introspectToken instead.")
                }
            val response =
                try {
                    texasWebClient
                        .post()
                        .uri(url)
                        .bodyValue(params)
                        .retrieve()
                        .awaitBody<TokenResponse.Success>()
                        .also {
                            log.debug("Hentet {}-token fra Texas", tokenEndpointType)
                        }
                } catch (e: WebClientResponseException) {
                    val error =
                        e.getResponseBodyAs(TokenErrorResponse::class.java) ?: TokenErrorResponse(
                            "Unknown error: ${e.responseBodyAsString}",
                            e.message,
                        )

                    TokenResponse.Error(error, e.statusCode)
                }

            when (response) {
                is TokenResponse.Success -> Token(response.accessToken)
                is TokenResponse.Error -> {
                    error(
                        "Feil ved henting av $tokenEndpointType-token fra Texas. Statuscode: ${response.status}. Error: ${response.error}",
                    )
                }
            }
        }
}

@Component
@Profile("!(mock-alt|testcontainers)")
class TexasClientImpl(
    texasWebClientBuilder: WebClient.Builder,
    @param:Value("\${nais.token.endpoint}")
    private val tokenEndpoint: String,
    @param:Value("\${nais.token.exchange.endpoint}")
    private val tokenXEndpoint: String,
) : TexasClient(texasWebClientBuilder, tokenEndpoint, tokenXEndpoint)

@Component
@Profile("mock-alt", "testcontainers")
class MockTexasClient(
    texasWebClientBuilder: WebClient.Builder,
    @param:Value("\${nais.token.endpoint}")
    private val tokenEndpoint: String,
    @param:Value("\${nais.token.exchange.endpoint}")
    private val tokenXEndpoint: String,
) : TexasClient(texasWebClientBuilder, tokenEndpoint, tokenXEndpoint) {
    override suspend fun getMaskinportenToken(): Token = Token("token")
}

sealed class TokenResponse {
    data class Success(
        @param:JsonProperty("access_token")
        val accessToken: String,
        @param:JsonProperty("expires_in")
        val expiresInSeconds: Int,
        @param:JsonProperty("token_type")
        val tokenType: String,
    ) : TokenResponse()

    data class Error(
        val error: TokenErrorResponse,
        val status: HttpStatusCode,
    ) : TokenResponse()
}

data class TokenErrorResponse(
    val error: String,
    @param:JsonProperty("error_description")
    val errorDescription: String,
)
