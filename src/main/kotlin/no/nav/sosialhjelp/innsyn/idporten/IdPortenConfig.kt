package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient

@Configuration
class IdPortenConfig(
    @Value("\${idporten_well_known_url}") private val wellKnownUrl: String,
    @Value("\${idporten_client_jwk}") private val clientJwk: String,
    @Value("\${idporten_redirect_uri}") private val redirectUri: String,
    @Value("\${idporten_clientid}") private val clientId: String,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) {

    @Bean
    fun idPortenProperties(): IdPortenProperties {
        return IdPortenProperties(
            wellKnown = wellknown,
            redirectUri = redirectUri,
            clientId = clientId,
            clientJwk = clientJwk
        )
    }

    private val idportenWebClient: WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .build()

    private val wellknown: IdPortenWellKnown
        get() = idportenWebClient.get()
            .uri(wellKnownUrl)
            .retrieve()
            .bodyToMono<IdPortenWellKnown>()
            .doOnSuccess { log.info("Hentet WellKnown for ID-porten") }
            .doOnError { log.warn("Feil ved henting av WellKnown for ID-porten", it) }
            .block() ?: throw RuntimeException("Feil ved henting av WellKnown for ID-porten")

    companion object {
        private val log by logger()
    }
}
