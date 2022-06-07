package no.nav.sosialhjelp.innsyn.client.maskinporten

import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient

@Configuration
class MaskinportenClientConfig(
    @Value("\${maskinporten_clientid}") private val clientId: String,
    @Value("\${maskinporten_scopes}") private val scopes: String,
    @Value("\${maskinporten_well_known_url}") private val wellKnownUrl: String,
    @Value("\${maskinporten_client_jwk}") private val clientJwk: String,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) {

    @Bean
    @Profile("!test")
    fun maskinportenClient(): MaskinportenClient {
        return MaskinportenClient(maskinportenWebClient, maskinportenProperties, wellknown)
    }

    @Bean
    @Profile("test")
    fun maskinportenClientTest(): MaskinportenClient {
        return MaskinportenClient(maskinportenWebClient, maskinportenProperties, WellKnown("issuer", "token_url"))
    }

    private val maskinportenWebClient: WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .build()

    private val maskinportenProperties = MaskinportenProperties(
        clientId = clientId,
        clientJwk = clientJwk,
        scope = scopes,
        wellKnownUrl = wellKnownUrl
    )

    private val wellknown: WellKnown
        get() = maskinportenWebClient.get()
            .uri(wellKnownUrl)
            .retrieve()
            .bodyToMono<WellKnown>()
            .doOnSuccess { log.info("Hentet WellKnown for Maskinporten") }
            .doOnError { log.warn("Feil ved henting av WellKnown for Maskinporten", it) }
            .block() ?: throw RuntimeException("Feil ved henting av WellKnown for Maskinporten")

    companion object {
        private val log by logger()
    }
}

data class WellKnown(
    val issuer: String,
    val token_endpoint: String,
)

data class MaskinportenProperties(
    val clientId: String,
    val clientJwk: String,
    val scope: String,
    val wellKnownUrl: String,
)
