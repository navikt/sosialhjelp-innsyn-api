package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.getProxiedReactorClientHttpConnector
import no.nav.sosialhjelp.innsyn.utils.getUnproxiedReactorClientHttpConnector
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NonProxiedWebClientConfig(
    private val webClientBuilder: WebClient.Builder
) {

    @Bean("nonProxiedWebClientBuilder")
    fun nonProxiedWebClientBuilder(): WebClient.Builder =
        webClientBuilder
            .clientConnector(getUnproxiedReactorClientHttpConnector())
}

@Profile("!(mock-alt|local|test)")
@Configuration
class ProxiedWebClientConfig(
    private val webClientBuilder: WebClient.Builder
) {

    @Value("\${HTTPS_PROXY}")
    private lateinit var proxyUrl: String

    @Bean("proxiedWebClientBuilder")
    fun proxiedWebClientBuilder(): WebClient.Builder =
        webClientBuilder
            .clientConnector(getProxiedReactorClientHttpConnector(proxyUrl))
}

@Profile("(mock-alt|local|test)")
@Configuration
class MockProxiedWebClientConfig(
    private val webClientBuilder: WebClient.Builder
) {

    @Bean("proxiedWebClientBuilder")
    fun proxiedWebClientBuilder(): WebClient.Builder =
        webClientBuilder
            .clientConnector(getUnproxiedReactorClientHttpConnector())
}
