package no.nav.sosialhjelp.innsyn.app.maskinporten

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

interface MaskinportenClient {
    suspend fun getToken(): String
}

@Component
class MaskinportenClientImpl(
    texasClientBuilder: WebClient.Builder,
    @Value("\${nais.token.endpoint}")
    private val tokenEndpoint: String,
) : MaskinportenClient {
    private val log by logger()

    private val texasClient =
        texasClientBuilder.defaultHeaders {
            it.contentType = MediaType.APPLICATION_JSON
        }.baseUrl(tokenEndpoint).codecs {
            it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
            it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
            it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
        }.build()

    override suspend fun getToken(): String =
        withContext(Dispatchers.IO + MDCContext()) {
            val response =
                try {
                    texasClient.post()
                        .bodyValue(params)
                        .retrieve()
                        .awaitBody<TokenResponse.Success>()
                        .also {
                            log.info("Hentet token fra Maskinporten (Texas)")
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
                is TokenResponse.Success -> response.accessToken
                is TokenResponse.Error -> {
                    error("Feil ved henting av token fra Maskinporten (Texas). Statuscode: ${response.status}. Error: ${response.error}")
                }
            }
        }

    private val params: Map<String, String>
        get() =
            mapOf("identity_provider" to "maskinporten", "target" to "ks:fiks")
}

sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Int,
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
