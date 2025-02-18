package no.nav.sosialhjelp.innsyn.app.config

import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient

object HttpClientUtil {
    fun getReactorClientHttpConnector(): ReactorClientHttpConnector {
        val httpClient: HttpClient = getHttpClient()
        return ReactorClientHttpConnector(httpClient)
    }

    fun getHttpClient() = HttpClient.create()
}
