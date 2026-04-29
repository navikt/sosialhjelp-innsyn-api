package no.nav.sosialhjelp.innsyn.soknad.api

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class SoknadApiClient(
    clientProperties: ClientProperties,
    webClientBuilder: WebClient.Builder
) {
    fun skalSkjuleOrginalSoknad(fiksDigisosId: String): Boolean =
        webClient.get()
            .uri("/soknad/hide/$fiksDigisosId")
            .retrieve()
            .bodyToMono<Boolean>()
            .block() ?: true

    private val webClient =
        webClientBuilder
            .baseUrl(clientProperties.soknadApiUrl)
            .build()



}
