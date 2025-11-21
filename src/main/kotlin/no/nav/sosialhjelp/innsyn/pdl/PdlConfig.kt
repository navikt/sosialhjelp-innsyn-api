package no.nav.sosialhjelp.innsyn.pdl

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.reactor.mono
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.client.HttpGraphQlClient
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class PdlConfig(
    private val clientProperties: ClientProperties,
    private val texasClient: TexasClient,
) {
    @Bean
    fun pdlWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.pdlEndpointUrl)
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient
                        .create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                        .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) },
                ),
            ).defaultHeader(IntegrationUtils.HEADER_BEHANDLINGSNUMMER, IntegrationUtils.BEHANDLINGSNUMMER_INNSYN)
            .filter(authorizationFilter())
            .build()

    private fun authorizationFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction { request, next ->
            mono {
                val tokenXToken = texasClient.getTokenXToken(clientProperties.pdlAudience, TokenUtils.getToken())
                ClientRequest
                    .from(request)
                    .header(HttpHeaders.AUTHORIZATION, tokenXToken.withBearer())
                    .build()
            }.flatMap { filteredRequest -> next.exchange(filteredRequest) }
        }

    @Bean
    fun pdlHttpGraphQlClient(pdlWebClient: WebClient): HttpGraphQlClient = HttpGraphQlClient.builder(pdlWebClient).build()
}
