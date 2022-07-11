package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil.getUnproxiedReactorClientHttpConnector
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class VirusScanConfig(
    @Value("\${innsyn.vedlegg.virusscan.url}") private val clamAvUrl: String
) {

    @Bean
    fun virusScanWebClient(webClientBuilder: WebClient.Builder) =
        webClientBuilder
            .baseUrl(clamAvUrl)
            .clientConnector(getUnproxiedReactorClientHttpConnector())
            .build()
}
