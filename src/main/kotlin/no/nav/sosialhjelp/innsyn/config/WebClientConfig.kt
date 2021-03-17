package no.nav.sosialhjelp.innsyn.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(HttpClient.newConnection()))
            .build()
}