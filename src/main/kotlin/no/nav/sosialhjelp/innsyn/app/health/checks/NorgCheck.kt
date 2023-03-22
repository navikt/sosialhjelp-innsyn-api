package no.nav.sosialhjelp.innsyn.app.health.checks

import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class NorgCheck(
    webClientBuilder: WebClient.Builder,
    clientProperties: ClientProperties
) : DependencyCheck {

    override val type = DependencyType.REST
    override val name = "Norg"
    override val address = clientProperties.norgUrl
    override val importance = Importance.WARNING

    private val norgWebClient = webClientBuilder
        .clientConnector(HttpClientUtil.getUnproxiedReactorClientHttpConnector())
        .build()

    override fun doCheck() {
        norgWebClient.get()
            .uri("$address/kodeverk/EnhetstyperNorg")
            .retrieve()
            .bodyToMono<String>()
            .onErrorMap(WebClientResponseException::class.java) { e ->
                log.warn("Ping - feilet mot Norg ${e.statusCode}", e)
                RuntimeException(e.message, e)
            }
            .block()
    }

    companion object {
        private val log by logger()
    }
}
