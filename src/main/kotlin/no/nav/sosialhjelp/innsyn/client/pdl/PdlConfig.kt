package no.nav.sosialhjelp.innsyn.client.pdl

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.resolver.DefaultAddressResolverGroup
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class PdlConfig(
    private val clientProperties: ClientProperties
) {
    @Bean
    fun pdlWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.pdlEndpointUrl)
            .defaultHeader(IntegrationUtils.HEADER_NAV_APIKEY, System.getenv(PDL_APIKEY))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.newConnection()
                        .resolver(DefaultAddressResolverGroup.INSTANCE)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                        .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) }
                )
            )
            .build()

    companion object {
        private const val PDL_APIKEY: String = "SOSIALHJELP_INNSYN_API_PDL_APIKEY_PASSWORD"
    }
}
