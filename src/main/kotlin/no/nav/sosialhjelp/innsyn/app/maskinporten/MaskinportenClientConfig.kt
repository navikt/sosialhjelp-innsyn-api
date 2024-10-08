package no.nav.sosialhjelp.innsyn.app.maskinporten

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
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
    protected val log by logger()

    @Bean
    fun maskinportenClient(): MaskinportenClient =
        runBlocking(MDCContext()) { MaskinportenClient(maskinportenWebClient, maskinportenProperties, getWellKnown()) }

    private val maskinportenWebClient: WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }.build()

    private val maskinportenProperties =
        MaskinportenProperties(
            clientId = clientId,
            clientJwk = clientJwk,
            scope = scopes,
            wellKnownUrl = wellKnownUrl,
        )

    private suspend fun getWellKnown(): WellKnown =
        withContext(Dispatchers.IO) {
            runCatching {
                maskinportenWebClient
                    .get()
                    .uri(wellKnownUrl)
                    .retrieve()
                    .awaitBody<WellKnown>()
            }.onSuccess {
                log.info("Hentet WellKnown for Maskinporten.")
            }.onFailure {
                log.warn("Feil ved henting av WellKnown for Maskinporten", it)
            }.getOrThrow()
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
