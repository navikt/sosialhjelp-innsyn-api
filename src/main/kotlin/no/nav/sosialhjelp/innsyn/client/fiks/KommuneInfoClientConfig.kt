package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.client.kommuneinfo.FiksProperties
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClientImpl
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.WebClient

@Profile("!mock")
@Configuration
class KommuneInfoClientConfig {

    @Bean
    fun kommuneInfoClient(clientProperties: ClientProperties): KommuneInfoClient {
        return KommuneInfoClientImpl(
                WebClient.create(),
                toFiksProperties(clientProperties)
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