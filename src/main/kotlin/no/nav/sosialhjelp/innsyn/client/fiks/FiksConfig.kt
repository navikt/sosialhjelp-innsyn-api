package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class FiksConfig(
    private val proxiedWebClientBuilder: WebClient.Builder,
    private val clientProperties: ClientProperties,
) {

    @Bean
    fun fiksWebClient(): WebClient =
        proxiedWebClientBuilder
            .baseUrl(clientProperties.fiksDigisosEndpointUrl)
            .codecs {
                it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
                it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .build()
}
