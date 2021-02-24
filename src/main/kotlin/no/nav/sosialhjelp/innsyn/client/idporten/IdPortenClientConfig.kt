package no.nav.sosialhjelp.innsyn.client.idporten

import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.idporten.client.IdPortenProperties
import no.nav.sosialhjelp.innsyn.utils.getenv
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Configuration
class IdPortenClientConfig(
        @Value("\${no.nav.sosialhjelp.idporten.token_url}") private val tokenUrl: String,
        @Value("\${no.nav.sosialhjelp.idporten.client_id}") private val clientId: String,
        @Value("\${no.nav.sosialhjelp.idporten.scope}") private val scope: String,
        @Value("\${no.nav.sosialhjelp.idporten.config_url}") private val configUrl: String,
        @Value("\${no.nav.sosialhjelp.idporten.truststore_type}") private val truststoreType: String,
        @Value("\${no.nav.sosialhjelp.idporten.truststore_filepath}") private val truststoreFilepath: String
) {

    @Bean
    fun idPortenClient(restTemplate: RestTemplate): IdPortenClient {
        return IdPortenClient(
                restTemplate = restTemplate,
                idPortenProperties = idPortenProperties()
        )
    }

    fun idPortenProperties(): IdPortenProperties {
        return IdPortenProperties(
                tokenUrl = tokenUrl,
                clientId = clientId,
                scope = scope,
                configUrl = configUrl,
                truststoreType = truststoreType,
                truststoreFilepath = truststoreFilepath,
                virksomhetSertifikatPath = getenv("VIRKSERT_STI", "/var/run/secrets/nais.io/virksomhetssertifikat")
        )
    }
}