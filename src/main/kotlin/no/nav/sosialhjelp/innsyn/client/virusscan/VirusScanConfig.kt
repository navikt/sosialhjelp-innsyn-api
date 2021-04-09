package no.nav.sosialhjelp.innsyn.client.virusscan

import no.nav.sosialhjelp.innsyn.utils.getUnproxiedReactorClientHttpConnector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class VirusScanConfig {

    @Bean
    fun virusScanWebClient(webClientBuilder: WebClient.Builder) =
        webClientBuilder
            .baseUrl(DEFAULT_CLAM_URI)
            .clientConnector(getUnproxiedReactorClientHttpConnector())
            .build()

    companion object {
        private const val DEFAULT_CLAM_URI = "http://clamav.nais.svc.nais.local/scan"
    }
}