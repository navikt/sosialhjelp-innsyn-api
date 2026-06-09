package no.nav.sosialhjelp.innsyn.soknad.api

import kotlinx.coroutines.reactor.awaitSingleOrNull
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import no.nav.sosialhjelp.innsyn.app.texas.TexasClient
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Configuration
class SoknadApiConfiguration(
    private val clientProperties: ClientProperties,
    private val webClientBuilder: WebClient.Builder,
    private val texasClient: TexasClient,
) {
    @Bean
    fun soknadApiClient(): SoknadApiClient? {
        if (clientProperties.soknadApiUrl.isNullOrBlank() || clientProperties.soknadApiAudience.isNullOrBlank()) {
            if (MiljoUtils.isRunningInProd()) {
                error("SoknadApiClient mangler url eller audience. Dette er påkrevd i produksjon")
            }
            return null
        }
        return SoknadApiClient(
            webClientBuilder
                .baseUrl(clientProperties.soknadApiUrl!!)
                .build(),
            texasClient,
            clientProperties.soknadApiAudience!!,
        )
    }
}

class SoknadApiClient(
    val webClient: WebClient,
    val texasClient: TexasClient,
    val target: String,
) {
    private val logger by logger()

    suspend fun skalSkjuleOriginalSoknad(fiksDigisosId: String): Boolean =
        runCatching {
            webClient
                .get()
                .uri("/soknad/hide/$fiksDigisosId")
                .header(HttpHeaders.AUTHORIZATION, texasClient.getTokenXToken(target, TokenUtils.getToken()).withBearer())
                .retrieve()
                .bodyToMono<Boolean>()
                .awaitSingleOrNull() ?: true
        }.onFailure {
            logger.warn("Failed fetching /soknad/hide/$fiksDigisosId from soknad-api", it)
        }.getOrDefault(true)
}
