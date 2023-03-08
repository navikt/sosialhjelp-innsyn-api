package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Profile("!prod-fss")
@Configuration
class DigisosApiTestConfig(
    private val clientProperties: ClientProperties,
) {

    @Bean
    fun digisosApiTestWebClient(webClientBuilder: WebClient.Builder, proxiedHttpClient: HttpClient) =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .defaultHeader(IntegrationUtils.HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonIdKommune)
            .defaultHeader(IntegrationUtils.HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonPassordKommune)
            .build()
}
