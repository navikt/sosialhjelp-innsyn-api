package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.app.tokendings.downloadWellKnown
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdPortenConfig(
    @Value("\${idporten_well_known_url}") private val idportenWellKnownUrl: String,
    @Value("\${idporten_client_jwk}") private val clientJwk: String,
    @Value("\${idporten_redirect_uri}") private val redirectUri: String,
    @Value("\${idporten_clientid}") private val clientId: String,
) {

    @Bean
    fun idPortenProperties(): IdPortenProperties {
        return IdPortenProperties(
            wellKnown = downloadWellKnown(idportenWellKnownUrl),
            redirectUri = redirectUri,
            clientId = clientId,
            clientJwk = clientJwk
        )
    }
}
