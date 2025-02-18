package no.nav.sosialhjelp.innsyn.navenhet

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NorgConfig(
    private val clientProperties: ClientProperties,
) {
    @Bean
    fun norgWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.norgUrl)
            .build()
}
