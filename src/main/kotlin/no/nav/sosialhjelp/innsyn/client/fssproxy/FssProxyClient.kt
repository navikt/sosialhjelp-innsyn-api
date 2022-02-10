package no.nav.sosialhjelp.innsyn.client.fssproxy

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil.getUnproxiedReactorClientHttpConnector
import no.nav.sosialhjelp.kotlin.utils.logger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FssProxyClient(
    webClientBuilder: WebClient.Builder,
    clientProperties: ClientProperties
) {

    private val fssProxyWebClient = webClientBuilder
        .clientConnector(getUnproxiedReactorClientHttpConnector())
        .baseUrl(clientProperties.fssProxyPingUrl)
        .build()

    fun ping() {
        fssProxyWebClient.options()
            .retrieve()
            .bodyToMono<String>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Ping - feilet mot fss-proxy ${e.statusCode}", e)
                RuntimeException(e.message, e)
            }
            .block()
    }

    companion object {
        private val log by logger()
    }
}
