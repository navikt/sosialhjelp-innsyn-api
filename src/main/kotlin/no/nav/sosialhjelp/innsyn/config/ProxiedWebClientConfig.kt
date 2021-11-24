package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.getProxiedReactorClientHttpConnector
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.getUnproxiedReactorClientHttpConnector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

// @Profile("!(mock-alt|local|test)")
@Configuration
class ProxiedWebClientConfig {

//    @Value("\${HTTPS_PROXY}")
    private val proxyUrl: String? = System.getenv("HTTPS_PROXY") ?: null

    @Bean
    fun proxiedWebClient(): WebClient =
        WebClient.builder()
            .clientConnector(getProxiedReactorClientHttpConnector(proxyUrl))
            .build()

    @Bean
    fun nonProxiedWebClient(): WebClient =
        WebClient.builder()
            .clientConnector(getUnproxiedReactorClientHttpConnector())
            .build()
}

// @Profile("(mock-alt|local|test)")
// @Configuration
// class MockProxiedWebClientConfig {
//
//    @Bean
//    fun proxiedWebClient(webClientBuilder: WebClient.Builder): WebClient =
//        webClientBuilder
//            .clientConnector(getUnproxiedReactorClientHttpConnector())
//            .build()
// }
