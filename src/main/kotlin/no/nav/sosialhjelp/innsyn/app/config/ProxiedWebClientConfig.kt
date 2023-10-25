package no.nav.sosialhjelp.innsyn.app.config

import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil.proxiedHttpClient
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil.unproxiedHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import reactor.netty.http.client.HttpClient

@Profile("(prod-fss|dev-fss)")
@Configuration
class ProxiedHttpClientConfig {
    @Value("\${HTTPS_PROXY}")
    private lateinit var proxyUrl: String

    @Bean
    fun proxiedHttpClient(): HttpClient = proxiedHttpClient(proxyUrl)
}

@Profile("!(prod-fss|dev-fss)")
@Configuration
class MockProxiedHttpClientConfig {
    @Bean
    fun proxiedHttpClient(): HttpClient = unproxiedHttpClient()
}
