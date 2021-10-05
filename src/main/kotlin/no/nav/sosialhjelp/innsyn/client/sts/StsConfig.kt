package no.nav.sosialhjelp.innsyn.client.sts

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.getUnproxiedReactorClientHttpConnector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.Base64

@Profile("!local")
@Configuration
class StsConfig(
    private val clientProperties: ClientProperties
) {

    @Bean
    fun stsWebClient(webClientBuilder: WebClient.Builder): WebClient =
        webClientBuilder
            .baseUrl(clientProperties.stsTokenEndpointUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic ${credentials()}")
            .defaultHeader(IntegrationUtils.HEADER_NAV_APIKEY, System.getenv(STSTOKEN_APIKEY))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .clientConnector(getUnproxiedReactorClientHttpConnector())
            .build()

    companion object {
        private fun credentials(): String =
            Base64.getEncoder().encodeToString("${System.getenv(SRVSOSIALHJELP_INNSYN_API_USERNAME)}:${System.getenv(SRVSOSIALHJELP_INNSYN_API_PASSWORD)}".toByteArray(Charsets.UTF_8))

        private const val SRVSOSIALHJELP_INNSYN_API_USERNAME: String = "SRVSOSIALHJELP_INNSYN_API_USERNAME"
        private const val SRVSOSIALHJELP_INNSYN_API_PASSWORD: String = "SRVSOSIALHJELP_INNSYN_API_PASSWORD"

        private const val STSTOKEN_APIKEY: String = "SOSIALHJELP_INNSYN_API_STSTOKEN_APIKEY_PASSWORD"
    }
}
