package no.nav.sosialhjelp.innsyn.client.norg

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class NorgConfig(
    private val clientProperties: ClientProperties,
) {

    @Bean
    fun norgWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.norgEndpointUrl)
            .clientConnector(ReactorClientHttpConnector(HttpClient.newConnection()))
            .build()
}