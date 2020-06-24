package no.nav.sbl.sosialhjelpinnsynapi.client.idporten

import no.nav.sbl.sosialhjelpinnsynapi.utils.getenv
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import no.nav.sosialhjelp.idporten.client.IdPortenProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Configuration
@EnableConfigurationProperties(IdPortenProperties::class)
class IdPortenClientConfig {

    @Bean
    fun idPortenClient(restTemplate: RestTemplate, idPortenProperties: IdPortenProperties): IdPortenClient {
        return IdPortenClient(
                restTemplate = restTemplate,
                idPortenProperties = idPortenProperties,
                virksomhetSertifikatPath = getenv("VIRKSERT_STI", "/var/run/secrets/nais.io/virksomhetssertifikat")
        )
    }
}