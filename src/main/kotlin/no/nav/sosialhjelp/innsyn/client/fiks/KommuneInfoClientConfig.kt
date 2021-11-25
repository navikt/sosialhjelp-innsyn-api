package no.nav.sosialhjelp.innsyn.client.fiks

import no.nav.sosialhjelp.client.kommuneinfo.FiksProperties
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClient
import no.nav.sosialhjelp.client.kommuneinfo.KommuneInfoClientImpl
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
class KommuneInfoClientConfig(
    private val clientProperties: ClientProperties
) {

    @Bean
    fun kommuneInfoClient(webClientBuilder: WebClient.Builder, proxiedHttpClient: HttpClient): KommuneInfoClient {
        return KommuneInfoClientImpl(
            webClientBuilder.clientConnector(ReactorClientHttpConnector(proxiedHttpClient)).build(),
            fiksProperties()
        )
    }

    private fun fiksProperties(): FiksProperties {
        return FiksProperties(
            clientProperties.fiksDigisosEndpointUrl + FiksPaths.PATH_KOMMUNEINFO,
            clientProperties.fiksDigisosEndpointUrl + FiksPaths.PATH_ALLE_KOMMUNEINFO,
            clientProperties.fiksIntegrasjonId,
            clientProperties.fiksIntegrasjonpassord
        )
    }
}
