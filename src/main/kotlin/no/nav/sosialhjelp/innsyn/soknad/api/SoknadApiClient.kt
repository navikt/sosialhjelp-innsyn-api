package no.nav.sosialhjelp.innsyn.soknad.api

import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class SoknadApiClient(
    clientProperties: ClientProperties,
    webClientBuilder: WebClient.Builder,
) {
    suspend fun skalSkjuleOrginalSoknad(fiksDigisosId: String): Boolean =
        webClient
            .get()
            .uri("/soknad/hide/$fiksDigisosId")
            .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
            .retrieve()
            .bodyToMono<Boolean>()
            .awaitSingleOrNull() ?: true

    private val webClient =
        webClientBuilder
            .baseUrl(clientProperties.soknadApiUrl)
            .build()
}
