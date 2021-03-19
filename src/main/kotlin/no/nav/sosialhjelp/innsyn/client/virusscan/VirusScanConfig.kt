package no.nav.sosialhjelp.innsyn.client.virusscan

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class VirusScanConfig {

    @Bean
    fun virusScanWebClient(webClientBuilder: WebClient.Builder) =
        webClientBuilder
            .baseUrl(DEFAULT_CLAM_URI)
            .clientConnector(ReactorClientHttpConnector(HttpClient.newConnection()))
            .build()

    companion object {
        private const val DEFAULT_CLAM_URI = "http://clamav.nais.svc.nais.local/scan"
    }
}