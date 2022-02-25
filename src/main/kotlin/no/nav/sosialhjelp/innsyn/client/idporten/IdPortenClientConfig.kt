package no.nav.sosialhjelp.innsyn.client.idporten

import no.nav.sosialhjelp.idporten.client.AccessToken
import no.nav.sosialhjelp.idporten.client.IdPortenAccessTokenResponse
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.idporten.client.IdPortenClientImpl
import no.nav.sosialhjelp.idporten.client.IdPortenProperties
import no.nav.sosialhjelp.innsyn.utils.getenv
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient

@Profile("!(mock-alt|test)")
@Configuration
class IdPortenClientConfig(
    @Value("\${no.nav.sosialhjelp.idporten.token_url}") private val tokenUrl: String,
    @Value("\${no.nav.sosialhjelp.idporten.client_id}") private val clientId: String,
    @Value("\${no.nav.sosialhjelp.idporten.scope}") private val scope: String,
    @Value("\${no.nav.sosialhjelp.idporten.config_url}") private val configUrl: String,
) {

    @Bean
    fun idPortenClient(webClientBuilder: WebClient.Builder, proxiedHttpClient: HttpClient): IdPortenClient {
        return IdPortenClientImpl(
            webClient = webClientBuilder.clientConnector(ReactorClientHttpConnector(proxiedHttpClient)).build(),
            idPortenProperties = idPortenProperties()
        )
    }

    fun idPortenProperties(): IdPortenProperties {
        return IdPortenProperties(
            tokenUrl = tokenUrl,
            clientId = clientId,
            scope = scope,
            configUrl = configUrl,
            virksomhetSertifikatPath = getenv("VIRKSERT_STI", "/var/run/secrets/nais.io/virksomhetssertifikat")
        )
    }
}

@Profile("(mock-alt|test)")
@Configuration
class IdPortenClientConfigMockAlt(
    @Value("\${no.nav.sosialhjelp.idporten.token_url}") private val tokenUrl: String
) {

    @Bean
    fun idPortenClient(webClientBuilder: WebClient.Builder, proxiedHttpClient: HttpClient): IdPortenClient {
        return IdPortenClientMockAlt(
            webClientBuilder.clientConnector(ReactorClientHttpConnector(proxiedHttpClient)).build(),
            tokenUrl
        )
    }

    private class IdPortenClientMockAlt(
        private val proxiedWebClient: WebClient,
        private val tokenUrl: String
    ) : IdPortenClient {

        override suspend fun requestToken(attempts: Int, headers: HttpHeaders): AccessToken {
            val response = proxiedWebClient.post()
                .uri(tokenUrl)
                .retrieve()
                .awaitBody<IdPortenAccessTokenResponse>()
            return AccessToken(response.accessToken, response.expiresIn)
        }
    }
}
