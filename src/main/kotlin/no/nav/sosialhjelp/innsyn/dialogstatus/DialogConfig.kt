package no.nav.sosialhjelp.innsyn.dialogstatus

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.unproxiedHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient

class DialogWebClient(val webClient: WebClient)

@Configuration
class DialogConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun dialogWebClient(webClientBuilder: WebClient.Builder): DialogWebClient {
        val builder = webClientBuilder
            .baseUrl(clientProperties.dialogEndpointUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(
                ReactorClientHttpConnector(
                    unproxiedHttpClient()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                        .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(60)) }
                )
            )

        return DialogWebClient(builder.build())
    }
}
