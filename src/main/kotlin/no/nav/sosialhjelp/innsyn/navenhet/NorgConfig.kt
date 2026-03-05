package no.nav.sosialhjelp.innsyn.navenhet

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MdcExchangeFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class NorgConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun norgWebClient(
        webClientBuilder: WebClient.Builder,
        httpClient: HttpClient,
    ): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
            .baseUrl(clientProperties.norgUrl)
            .filter(MdcExchangeFilter)
            .build()
}
