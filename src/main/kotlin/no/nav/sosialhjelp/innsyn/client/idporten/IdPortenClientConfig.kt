package no.nav.sosialhjelp.innsyn.client.idporten

import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.idporten.client.IdPortenProperties
import no.nav.sosialhjelp.innsyn.utils.getenv
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class IdPortenClientConfig(
    private val proxiedWebClient: WebClient,
    @Value("\${no.nav.sosialhjelp.idporten.token_url}") private val tokenUrl: String,
    @Value("\${no.nav.sosialhjelp.idporten.client_id}") private val clientId: String,
    @Value("\${no.nav.sosialhjelp.idporten.scope}") private val scope: String,
    @Value("\${no.nav.sosialhjelp.idporten.config_url}") private val configUrl: String,
) {

    @Bean
    fun idPortenClient(): IdPortenClient {
        return IdPortenClient(
            webClient = proxiedWebClient,
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
