package no.nav.sosialhjelp.innsyn.klage

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.configureCodecs
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class MellomlagerConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun mellomlagerWebClient(
        webClientBuilder: WebClient.Builder,
        httpClient: HttpClient,
    ): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .configureCodecs()
            .defaultHeader(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .defaultHeader(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .build()
}
