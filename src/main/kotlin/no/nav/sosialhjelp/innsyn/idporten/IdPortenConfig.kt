package no.nav.sosialhjelp.innsyn.idporten

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient

@Profile("idporten")
@Configuration
class IdPortenConfig(
    @Value("\${idporten_well_known_url}") private val wellKnownUrl: String,
    @Value("\${idporten_client_jwk}") private val clientJwk: String,
    @Value("\${idporten_redirect_uri}") private val redirectUri: String,
    @Value("\${idporten_clientid}") private val clientId: String,
    @Value("\${idporten_post_logout_redirect_uri}") private val postLogoutRedirectUri: String,
    @Value("\${idporten_login_timeout}") private val loginTimeout: Long,
    @Value("\${idporten_session_timeout}") private val sessionTimeout: Long,
    @Value("\${idporten_token_timeout}") private val tokenTimeout: Long,
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) {
    @Bean
    fun idPortenProperties(): IdPortenProperties =
        runBlocking(MDCContext()) {
            IdPortenProperties(
                wellKnown = getWellKnown(),
                redirectUri = redirectUri,
                clientId = clientId,
                clientJwk = clientJwk,
                postLogoutRedirectUri = postLogoutRedirectUri,
                loginTimeout = loginTimeout,
                sessionTimeout = sessionTimeout,
                tokenTimeout = tokenTimeout,
            )
        }

    private val idportenWebClient: WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .build()

    private suspend fun getWellKnown(): IdPortenWellKnown =
        withContext(Dispatchers.IO) {
            runCatching {
                idportenWebClient.get()
                    .uri(wellKnownUrl)
                    .retrieve()
                    .awaitBody<IdPortenWellKnown>()
            }.onSuccess {
                log.info("Hentet WellKnown for ID-porten")
            }.onFailure {
                log.warn("Feil ved henting av WellKnown for ID-porten", it)
            }.getOrThrow()
        }

    companion object {
        private val log by logger()
    }
}
