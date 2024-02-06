package no.nav.sosialhjelp.innsyn.saksoversikt.soknadapi

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.mdcExchangeFilter
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getUserIdFromToken
import no.nav.sosialhjelp.innsyn.app.tokendings.TokendingsService
import no.nav.sosialhjelp.innsyn.saksoversikt.SaksListeResponse
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class SoknadApiClient(
    private val clientProperties: ClientProperties,
    private val tokendingsService: TokendingsService,
    webClientBuilder: WebClient.Builder,
) {
    private val soknadApiWebClient =
        webClientBuilder
            .clientConnector(HttpClientUtil.getUnproxiedReactorClientHttpConnector())
            .baseUrl(clientProperties.soknadApiUrl)
            .filter(mdcExchangeFilter)
            .build()

    suspend fun getSvarUtSoknader(token: String): List<SaksListeResponse> {
        return soknadApiWebClient.get()
            .uri("/soknadoversikt/soknader")
            .accept(MediaType.APPLICATION_JSON)
            .header(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
            .header(AUTHORIZATION, BEARER + tokenXtoken(token))
            .retrieve()
            .awaitBodyOrNull<List<SaksListeResponse>>() ?: emptyList()
    }

    private suspend fun tokenXtoken(token: String) =
        tokendingsService.exchangeToken(getUserIdFromToken(), token, clientProperties.soknadApiAudience)
}
