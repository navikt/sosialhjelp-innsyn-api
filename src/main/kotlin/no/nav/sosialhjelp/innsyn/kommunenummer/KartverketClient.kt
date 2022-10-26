package no.nav.sosialhjelp.innsyn.kommunenummer

import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient

@Component
class KartverketClient(
    webClientBuilder: WebClient.Builder,
    proxiedHttpClient: HttpClient,
) {

    fun getKommunenummer(): String {
        return kartverketWebClient.get()
            .uri(KARTVERKET_URL)
            .retrieve()
            .bodyToMono<String>()
            .block()
            ?: throw RuntimeException("Noe feilet ved henting av kommunenummer fra Kartverket")
    }

    private val kartverketWebClient: WebClient =
        webClientBuilder
            .clientConnector(ReactorClientHttpConnector(proxiedHttpClient))
            .codecs {
                it.defaultCodecs().maxInMemorySize(1 * 1024 * 1024)
            }
            .build()

    companion object {
        private const val KARTVERKET_URL = "https://register.geonorge.no/api/subregister/sosi-kodelister/kartverket/kommunenummer-alle.json"
    }
}
