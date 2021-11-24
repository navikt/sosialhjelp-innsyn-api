package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.getProxiedReactorClientHttpConnector
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.getUnproxiedReactorClientHttpConnector
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Profile("!(mock-alt|local|test)")
@Configuration
class ProxiedWebClientConfig {

    @Value("\${HTTPS_PROXY}")
    private lateinit var proxyUrl: String

    @Bean
    fun proxiedWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .clientConnector(getProxiedReactorClientHttpConnector(proxyUrl))
            .build()
}

@Profile("(mock-alt|local|test)")
@Configuration
class MockProxiedWebClientConfig {

    @Bean
    fun proxiedWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .clientConnector(getUnproxiedReactorClientHttpConnector())
            .build()
}
