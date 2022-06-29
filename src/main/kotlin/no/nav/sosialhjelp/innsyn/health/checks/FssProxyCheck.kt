package no.nav.sosialhjelp.innsyn.health.checks

import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.utils.HttpClientUtil
import no.nav.sosialhjelp.kotlin.utils.logger
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FssProxyCheck(
    webClientBuilder: WebClient.Builder,
    clientProperties: ClientProperties
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "FssProxy"
    override val address = clientProperties.fssProxyPingUrl
    override val importance = Importance.WARNING

    private val fssProxyWebClient = webClientBuilder
        .clientConnector(HttpClientUtil.getUnproxiedReactorClientHttpConnector())
        .baseUrl(clientProperties.fssProxyPingUrl)
        .build()

    override fun doCheck() {
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
