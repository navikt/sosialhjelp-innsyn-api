package no.nav.sosialhjelp.innsyn.client.norg

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NorgConfig(
    private val clientProperties: ClientProperties,
) {

    @Bean
    fun norgWebClient(nonProxiedWebClientBuilder: WebClient.Builder): WebClient =
        nonProxiedWebClientBuilder
            .baseUrl(clientProperties.norgEndpointUrl)
            .build()
}
