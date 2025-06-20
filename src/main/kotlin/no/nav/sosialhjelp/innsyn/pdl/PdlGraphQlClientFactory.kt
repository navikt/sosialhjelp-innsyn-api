package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.Token
import org.springframework.graphql.client.HttpGraphQlClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PdlGraphQlClientFactory(
    private val pdlWebClient: WebClient,
    private val texasClient: TexasClient,
    private val clientProperties: ClientProperties,
) {
    suspend fun getClient(token: Token): HttpGraphQlClient =
        HttpGraphQlClient
            .builder(pdlWebClient)
            .header("Authorization", tokenXtoken(token).withBearer())
            .build()

    private suspend fun tokenXtoken(token: Token) = texasClient.getTokenXToken(clientProperties.pdlAudience, token)
}
