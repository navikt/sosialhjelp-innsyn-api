package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.getProxiedReactorClientHttpConnector
import no.nav.sosialhjelp.innsyn.utils.getUnproxiedReactorClientHttpConnector
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Profile("!(mock-alt|local)")
@Configuration
class ProxiedWebClientConfig {

    @Value("\${HTTPS_PROXY}")
    private lateinit var proxyUrl: String

    @Bean
    fun proxiedWebClientBuilder(): WebClient.Builder =
        WebClient.builder()
            .clientConnector(getProxiedReactorClientHttpConnector(proxyUrl))
}

@Profile("(mock-alt|local)")
@Configuration
class MockProxiedWebClientConfig {

    @Bean
    fun proxiedWebClientBuilder(): WebClient.Builder =
        WebClient.builder()
            .clientConnector(getUnproxiedReactorClientHttpConnector())
}
