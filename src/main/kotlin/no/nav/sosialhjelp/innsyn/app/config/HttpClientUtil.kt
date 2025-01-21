package no.nav.sosialhjelp.innsyn.app.config

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient

object HttpClientUtil {
    fun getReactorClientHttpConnector(): ReactorClientHttpConnector {
        val httpClient: HttpClient = getHttpClient()
        return ReactorClientHttpConnector(httpClient)
    }

    fun getHttpClient() =
        HttpClient
            .newConnection()
            .resolver(DefaultAddressResolverGroup.INSTANCE)
}
