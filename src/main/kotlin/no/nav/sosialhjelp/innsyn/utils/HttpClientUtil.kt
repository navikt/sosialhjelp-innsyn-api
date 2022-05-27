package no.nav.sosialhjelp.innsyn.utils

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient

object HttpClientUtil {

    fun getUnproxiedReactorClientHttpConnector(): ReactorClientHttpConnector {
        val httpClient: HttpClient = unproxiedHttpClient()
        return ReactorClientHttpConnector(httpClient)
    }

    fun unproxiedHttpClient() = HttpClient
        .newConnection()
        .resolver(DefaultAddressResolverGroup.INSTANCE)
}
