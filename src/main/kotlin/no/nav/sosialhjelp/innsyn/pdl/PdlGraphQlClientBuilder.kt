package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.mdcExchangeFilter
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEHANDLINGSNUMMER_INNSYN
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_BEHANDLINGSNUMMER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import org.springframework.graphql.client.HttpGraphQlClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PdlGraphQlClientBuilder(
    private val webClientBuilder: WebClient.Builder,
    private val texasClient: TexasClient,
    private val clientProperties: ClientProperties,
) {
    suspend fun buildClient(token: String): HttpGraphQlClient =
        HttpGraphQlClient.builder(
            webClientBuilder
                .baseUrl(clientProperties.pdlEndpointUrl)
                .filter(mdcExchangeFilter)
                .build()
        )
            .header(HEADER_BEHANDLINGSNUMMER, BEHANDLINGSNUMMER_INNSYN)
            .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
            .header("Authorization", "Bearer ${tokenXtoken(token)}")
            .build()

    private suspend fun tokenXtoken(token: String) = texasClient.getTokenXToken(clientProperties.pdlAudience, token)

}
