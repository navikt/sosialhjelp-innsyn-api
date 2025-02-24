package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.mdcExchangeFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PdlWebClientConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun pdlWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder.baseUrl(clientProperties.pdlEndpointUrl).filter(mdcExchangeFilter).build()
}
