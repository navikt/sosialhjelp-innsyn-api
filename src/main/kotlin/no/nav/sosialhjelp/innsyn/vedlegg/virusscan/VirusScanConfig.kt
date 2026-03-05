package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MdcExchangeFilter
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
        defaultHttpClient: HttpClient,
    ) = webClientBuilder
        .clientConnector(ReactorClientHttpConnector(defaultHttpClient))
        .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) }
        .baseUrl(clamAvUrl)
        .filter(MdcExchangeFilter)
        .build()
}
