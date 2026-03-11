package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class VirusScanConfig(
    @param:Value("\${innsyn.vedlegg.virusscan.url}") private val clamAvUrl: String,
) {
    @Bean
    fun virusScanWebClient(
        webClientBuilder: WebClient.Builder,
        httpClient: HttpClient,
    ) = webClientBuilder
        .clientConnector(ReactorClientHttpConnector(httpClient))
        .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
        .baseUrl(clamAvUrl)
        .build()
}
