package no.nav.sosialhjelp.innsyn.app.config

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient

object HttpClientUtil {
    fun getReactorClientHttpConnector(): ReactorClientHttpConnector {
        val httpClient: HttpClient = httpClient()
        return ReactorClientHttpConnector(httpClient)
    }

    fun httpClient() =
        HttpClient
            .newConnection()
            .resolver(DefaultAddressResolverGroup.INSTANCE)
}
