package no.nav.sosialhjelp.innsyn.tilgang.pdl

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.unproxiedHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PdlConfig(
    private val clientProperties: ClientProperties
) {
    @Bean
    fun pdlWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.pdlEndpointUrl)
            .clientConnector(
                ReactorClientHttpConnector(
                    unproxiedHttpClient()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                        .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) }
                )
            )
            .build()
}
