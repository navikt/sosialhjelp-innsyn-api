package no.nav.sbl.sosialhjelpinnsynapi.client.idporten

import no.nav.sbl.sosialhjelpinnsynapi.utils.getenv
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.idporten.client.IdPortenProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Configuration
class IdPortenClientConfig(
        @Value("\${idporten.token_url}") private val tokenUrl: String,
        @Value("\${idporten.client_id}") private val clientId: String,
        @Value("\${idporten.scope}") private val scope: String,
        @Value("\${idporten.config_url}") private val configUrl: String,
        @Value("\${idporten.truststore_type}") private val truststoreType: String,
        @Value("\${idporten.truststore_filepath}") private val truststoreFilepath: String
) {

    @Bean
    fun idPortenClient(restTemplate: RestTemplate): IdPortenClient {
        return IdPortenClient(
                restTemplate = restTemplate,
                properties = IdPortenProperties(
                        tokenUrl,
                        clientId,
                        scope,
                        configUrl,
                        getenv("VIRKSERT_STI", "/var/run/secrets/nais.io/virksomhetssertifikat"),
                        truststoreType,
                        truststoreFilepath
                )
        )
    }
}