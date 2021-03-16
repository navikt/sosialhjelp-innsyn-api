package no.nav.sosialhjelp.innsyn.utils

import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import java.net.URL

fun getReactorClientHttpConnector(proxyUrl: String): ReactorClientHttpConnector {
    val uri = URL(proxyUrl)

    val httpClient: HttpClient = HttpClient.create()
        .proxy { proxy ->
            proxy.type(ProxyProvider.Proxy.HTTP).host(uri.host).port(uri.port)
        }

    return ReactorClientHttpConnector(httpClient)
}