package no.nav.sosialhjelp.innsyn.client.dialog

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.resolver.DefaultAddressResolverGroup
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

class DialogWebClient(val webClient: WebClient)

@Configuration
class DialogConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun dialogWebClient(nonProxiedWebClientBuilder: WebClient.Builder): DialogWebClient {
        val builder = nonProxiedWebClientBuilder
            .baseUrl(clientProperties.dialogEndpointUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.newConnection()
                        .resolver(DefaultAddressResolverGroup.INSTANCE)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                        .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(60)) }
                )
            )

        return DialogWebClient(builder.build())
    }
}
