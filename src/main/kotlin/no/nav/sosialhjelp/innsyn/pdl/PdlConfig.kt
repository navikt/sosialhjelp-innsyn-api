package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class PdlConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun pdlWebClient(
        webClientBuilder: WebClient.Builder,
        httpClient: HttpClient,
    ): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(clientProperties.pdlEndpointUrl)
            .defaultHeader(IntegrationUtils.HEADER_BEHANDLINGSNUMMER, IntegrationUtils.BEHANDLINGSNUMMER_INNSYN)
            .build()
}
