package no.nav.sosialhjelp.innsyn.digisosapi

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class FiksConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun fiksWebClient(
        webClientBuilder: WebClient.Builder,
        httpClient: HttpClient,
    ): WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .defaultHeader(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            .defaultHeader(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            .build()
}
