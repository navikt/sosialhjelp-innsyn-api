package no.nav.sosialhjelp.innsyn.client.tokendings

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.kotlin.utils.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
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
    @Profile("!test")
    fun tokendingsWebClient(nonProxiedWebClientBuilder: WebClient.Builder): TokendingsWebClient {
        val wellKnown = downloadWellKnown(clientProperties.tokendingsUrl)
        log.info("TokendingsClient: Lastet ned well known fra: ${clientProperties.tokendingsUrl} bruker token endpoint: ${wellKnown.tokenEndpoint}")
        return TokendingsWebClient(
            buildWebClient(nonProxiedWebClientBuilder, wellKnown.tokenEndpoint, applicationFormUrlencodedHeaders()),
            wellKnown
        )
    }

    @Bean
    @Profile("test")
    fun tokendingsWebClientTest(nonProxiedWebClientBuilder: WebClient.Builder): TokendingsWebClient {
        log.info("TokendingsClient: Setter opp test client som bruker token endpoint: ${clientProperties.tokendingsUrl}")
        return TokendingsWebClient(
            buildWebClient(nonProxiedWebClientBuilder, clientProperties.tokendingsUrl),
            WellKnown("iss-localhost", "authorizationEndpoint", "tokenEndpoint", clientProperties.tokendingsUrl)
        )
    }

    companion object {
        private val log by logger()
    }
}
