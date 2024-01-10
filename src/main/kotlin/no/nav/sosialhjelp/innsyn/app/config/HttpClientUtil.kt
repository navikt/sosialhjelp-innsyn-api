package no.nav.sosialhjelp.innsyn.app.config

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.net.URI

object HttpClientUtil {
    fun proxiedHttpClient(proxyUrl: String): HttpClient {
        val uri = URI.create(proxyUrl)

        val httpClient: HttpClient =
            HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .proxy { proxy ->
                    proxy.type(ProxyProvider.Proxy.HTTP).host(uri.host).port(uri.port)
                }
        return httpClient
    }

    fun getUnproxiedReactorClientHttpConnector(): ReactorClientHttpConnector {
        val httpClient: HttpClient = unproxiedHttpClient()
        return ReactorClientHttpConnector(httpClient)
    }

    fun unproxiedHttpClient() =
        HttpClient
            .newConnection()
            .resolver(DefaultAddressResolverGroup.INSTANCE)
}
