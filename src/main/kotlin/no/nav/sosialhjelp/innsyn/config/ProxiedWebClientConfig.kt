package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.getReactorClientHttpConnector
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Profile("!(mock|mock-alt|local)")
@Configuration
class ProxiedWebClientConfig {

    @Value("\${HTTPS_PROXY}")
    private lateinit var proxyUrl: String

    @Bean
    fun proxiedWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .clientConnector(getReactorClientHttpConnector(proxyUrl))
            .build()

}

@Profile("mock|mock-alt|local")
@Configuration
class MockProxiedWebClientConfig {

    @Bean
    fun proxiedWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(HttpClient.newConnection()))
            .build()

}