package no.nav.sosialhjelp.innsyn.pdl

import io.netty.channel.ChannelOption
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.mdcExchangeFilter
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil.getHttpClient
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEHANDLINGSNUMMER_INNSYN
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_BEHANDLINGSNUMMER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import org.springframework.graphql.client.HttpGraphQlClient
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Component
class PdlGraphQlClientBuilder(
    webClientBuilder: WebClient.Builder,
    private val texasClient: TexasClient,
    private val clientProperties: ClientProperties,
) {
    suspend fun buildClient(token: String): HttpGraphQlClient = pdlGraphClientBuilder
        .header(HEADER_BEHANDLINGSNUMMER, BEHANDLINGSNUMMER_INNSYN)
        .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
        .header("Authorization", "Bearer ${tokenXtoken(token)}")
        .build()

    private suspend fun tokenXtoken(token: String) =
        texasClient.getTokenXToken(clientProperties.pdlAudience, token)

    private val reactiveClient = ReactorClientHttpConnector(
        getHttpClient()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
            .responseTimeout(Duration.ofSeconds(30)),
    )

    private val pdlGraphClientBuilder =
        HttpGraphQlClient.builder(
            webClientBuilder
                .baseUrl(clientProperties.pdlEndpointUrl)
                .clientConnector(reactiveClient)
                .filter(mdcExchangeFilter),
        )
}
