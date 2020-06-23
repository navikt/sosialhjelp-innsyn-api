package no.nav.sbl.sosialhjelpinnsynapi.client.idporten

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.utils.getenv
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.idporten.client.IdPortenProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Configuration
class IdPortenClientConfig {

    @Bean
    fun idPortenClient(restTemplate: RestTemplate, clientProperties: ClientProperties): IdPortenClient {
        return IdPortenClient(restTemplate, toIdPortenProperties(clientProperties))
    }

    private fun toIdPortenProperties(clientProperties: ClientProperties): IdPortenProperties {
        return IdPortenProperties(
                clientProperties.idPortenTokenUrl,
                clientProperties.idPortenClientId,
                clientProperties.idPortenScope,
                clientProperties.idPortenConfigUrl,
                getenv("VIRKSERT_STI", "/var/run/secrets/nais.io/virksomhetssertifikat"),
                clientProperties.idPortenTruststoreType,
                clientProperties.idPortenTruststoreFilePath
        )
    }
}