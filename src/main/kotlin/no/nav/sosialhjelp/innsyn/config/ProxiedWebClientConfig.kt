package no.nav.sosialhjelp.innsyn.config

import io.netty.resolver.DefaultAddressResolverGroup
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.unproxiedHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.net.URL

@Profile("!(dev|mock-alt|local|test)")
@Configuration
class ProxiedHttpClientConfig {

    @Value("\${HTTPS_PROXY}")
    private lateinit var proxyUrl: String

    @Bean
    fun proxiedHttpClient(): HttpClient = proxiedHttpClient(proxyUrl)

    private fun proxiedHttpClient(proxyUrl: String): HttpClient {
        val uri = URL(proxyUrl)

        val httpClient: HttpClient = HttpClient.create()
            .resolver(DefaultAddressResolverGroup.INSTANCE)
            .proxy { proxy ->
                proxy.type(ProxyProvider.Proxy.HTTP).host(uri.host).port(uri.port)
            }
        return httpClient
    }
}

@Profile("(dev|mock-alt|local|test)")
@Configuration
class MockProxiedHttpClientConfig {

    @Bean
    fun proxiedHttpClient(): HttpClient = unproxiedHttpClient()
}
