package no.nav.sosialhjelp.innsyn.tilgang.pdl

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.mdcExchangeFilter
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil.httpClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PdlConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun pdlWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.pdlEndpointUrl)
            .clientConnector(
                ReactorClientHttpConnector(
                    httpClient()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                        .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) },
                ),
            )
            .filter(mdcExchangeFilter)
            .defaultHeader(IntegrationUtils.HEADER_BEHANDLINGSNUMMER, IntegrationUtils.BEHANDLINGSNUMMER_INNSYN)
            .build()
}
