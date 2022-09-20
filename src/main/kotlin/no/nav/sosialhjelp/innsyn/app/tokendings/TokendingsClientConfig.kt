package no.nav.sosialhjelp.innsyn.app.tokendings

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

class TokendingsWebClient(
    val webClient: WebClient,
    val wellKnown: WellKnown
)

@Configuration
class TokendingsClientConfig(
    private val clientProperties: ClientProperties
) {
    @Bean
    fun tokendingsWebClient(webClientBuilder: WebClient.Builder): TokendingsWebClient {
        val wellKnown = downloadWellKnown(clientProperties.tokendingsUrl)
        log.info("TokendingsClient: Lastet ned well known fra: ${clientProperties.tokendingsUrl} bruker token endpoint: ${wellKnown.tokenEndpoint}")
        return TokendingsWebClient(
            buildWebClient(webClientBuilder, wellKnown.tokenEndpoint, applicationFormUrlencodedHeaders()),
            wellKnown
        )
    }

    companion object {
        private val log by logger()
    }
}
