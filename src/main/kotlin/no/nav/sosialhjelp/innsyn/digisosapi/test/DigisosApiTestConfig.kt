package no.nav.sosialhjelp.innsyn.digisosapi.test

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Profile("!prodgcp")
@Configuration
class DigisosApiTestConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun digisosApiTestWebClient(
        webClientBuilder: WebClient.Builder,
        httpClient: HttpClient,
    ) = webClientBuilder
        .clientConnector(ReactorClientHttpConnector(httpClient))
        .baseUrl(clientProperties.fiksDigisosEndpointUrl)
        .codecs {
            it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        }.defaultHeader(IntegrationUtils.HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonIdKommune)
        .defaultHeader(IntegrationUtils.HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonPassordKommune)
        .build()
}
