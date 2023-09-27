package no.nav.sosialhjelp.innsyn.app.maskinporten

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

sealed class MaskinportenClientConfig(
    clientId: String,
    scopes: String,
    private val wellKnownUrl: String,
    clientJwk: String,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) {

    private val log by logger()
    abstract fun maskinportenClient(): MaskinportenClient

    protected val maskinportenWebClient: WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .build()

    protected val maskinportenProperties = MaskinportenProperties(
        clientId = clientId,
        clientJwk = clientJwk,
        scope = scopes,
        wellKnownUrl = wellKnownUrl
    )

    protected val wellknown: WellKnown
        get() = maskinportenWebClient.get()
            .uri(wellKnownUrl)
            .retrieve()
            .bodyToMono<WellKnown>()
            .doOnSuccess { log.info("Hentet WellKnown for Maskinporten.") }
            .doOnError { log.warn("Feil ved henting av WellKnown for Maskinporten", it) }
            .block() ?: throw RuntimeException("Feil ved henting av WellKnown for Maskinporten")
}

@Configuration
class MaskinportenClientConfigImpl(
    @Value("\${maskinporten_clientid}") clientId: String,
    @Value("\${maskinporten_scopes}") scopes: String,
    @Value("\${maskinporten_well_known_url}") wellKnownUrl: String,
    @Value("\${maskinporten_client_jwk}") clientJwk: String,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) : MaskinportenClientConfig(clientId, scopes, wellKnownUrl, clientJwk, webClientBuilder, proxiedHttpClient) {

    @Bean("maskinportenClient")
    override fun maskinportenClient(): MaskinportenClient = MaskinportenClient(maskinportenWebClient, maskinportenProperties, wellknown)
}

@Configuration
@Profile("!local")
class SpecialMaskinportenClientConfig(
    @Value("\${special_maskinporten_clientid}") clientId: String,
    @Value("\${special_maskinporten_scopes}") scopes: String,
    @Value("\${special_maskinporten_well_known_url}") wellKnownUrl: String,
    @Value("\${special_maskinporten_client_jwk}") clientJwk: String,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) : MaskinportenClientConfig(clientId, scopes, wellKnownUrl, clientJwk, webClientBuilder, proxiedHttpClient) {
    @Bean("specialMaskinportenClient")
    override fun maskinportenClient(): MaskinportenClient = MaskinportenClient(maskinportenWebClient, maskinportenProperties, wellknown)
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
