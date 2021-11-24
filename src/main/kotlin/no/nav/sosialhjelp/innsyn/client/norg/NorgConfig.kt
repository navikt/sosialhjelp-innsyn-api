package no.nav.sosialhjelp.innsyn.client.norg

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.getUnproxiedReactorClientHttpConnector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NorgConfig(
    private val clientProperties: ClientProperties,
) {

    @Bean
    fun norgWebClient(nonProxiedWebClient: WebClient): WebClient =
        nonProxiedWebClient.mutate()
            .baseUrl(clientProperties.norgEndpointUrl)
            .clientConnector(getUnproxiedReactorClientHttpConnector())
            .build()
}
