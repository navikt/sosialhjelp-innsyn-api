package no.nav.sbl.sosialhjelpinnsynapi.client.fiks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sosialhjelp.client.kommuneinfo.FiksProperties
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClientImpl
import no.nav.sosialhjelp.idporten.client.IdPortenClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Configuration
class KommuneInfoClientConfig {

    @Bean
    fun kommuneInfoClient(restTemplate: RestTemplate, clientProperties: ClientProperties, idPortenClient: IdPortenClient): KommuneInfoClient {
        return KommuneInfoClientImpl(
                restTemplate,
                toFiksProperties(clientProperties),
                idPortenClient
        )
    }

    private fun toFiksProperties(clientProperties: ClientProperties): FiksProperties {
        return FiksProperties(
                clientProperties.fiksDigisosEndpointUrl + FiksPaths.PATH_KOMMUNEINFO,
                clientProperties.fiksDigisosEndpointUrl + FiksPaths.PATH_ALLE_KOMMUNEINFO,
                clientProperties.fiksIntegrasjonId,
                clientProperties.fiksIntegrasjonpassord
        )
    }

}