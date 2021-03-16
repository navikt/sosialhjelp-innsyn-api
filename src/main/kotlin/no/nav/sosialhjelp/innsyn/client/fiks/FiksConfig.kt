package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class FiksConfig (
    private val proxiedWebClient: WebClient,
    private val clientProperties: ClientProperties
) {

    @Bean
    fun fiksWebClient(): WebClient =
        proxiedWebClient
            .mutate()
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .build()
}