package no.nav.sosialhjelp.innsyn.soknad.api

import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Configuration
class SoknadApiConfiguration(
    private val clientProperties: ClientProperties,
    private val webClientBuilder: WebClient.Builder,
) {
    @Bean
    fun soknadApiClient(): SoknadApiClient? {
        if (clientProperties.soknadApiUrl.isNullOrBlank()) {
            if (MiljoUtils.isRunningInProd()) {
                error("SoknadApiClient mangler url. Dette er påkrevd i produksjon")
            }
            return null
        }
        return SoknadApiClient(
            webClientBuilder
                .baseUrl(clientProperties.soknadApiUrl!!)
                .build(),
        )
    }
}

class SoknadApiClient(
    val webClient: WebClient,
) {
    suspend fun skalSkjuleOriginalSoknad(fiksDigisosId: String): Boolean =
        webClient
            .get()
            .uri("/soknad/hide/$fiksDigisosId")
            .header(HttpHeaders.AUTHORIZATION, TokenUtils.getToken().withBearer())
            .retrieve()
            .bodyToMono<Boolean>()
            .awaitSingleOrNull() ?: true
}
