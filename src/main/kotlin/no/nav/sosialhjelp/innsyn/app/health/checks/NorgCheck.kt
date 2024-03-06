package no.nav.sosialhjelp.innsyn.app.health.checks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.selftest.DependencyCheck
import no.nav.sosialhjelp.selftest.DependencyType
import no.nav.sosialhjelp.selftest.Importance
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBodilessEntity

@Component
class NorgCheck(
    webClientBuilder: WebClient.Builder,
    clientProperties: ClientProperties,
) : DependencyCheck {
    override val type = DependencyType.REST
    override val name = "Norg"
    override val address = clientProperties.norgUrl
    override val importance = Importance.WARNING

    private val norgWebClient =
        webClientBuilder
            .clientConnector(HttpClientUtil.getUnproxiedReactorClientHttpConnector())
            .build()

    override fun doCheck(): Unit =
        runBlocking(MDCContext()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    norgWebClient.get()
                        .uri("$address/kodeverk/EnhetstyperNorg")
                        .retrieve()
                        .awaitBodilessEntity()
                }.onFailure {
                    if (it is WebClientResponseException) {
                        log.warn("Ping - feilet mot Norg ${it.statusCode}", it)
                    }
                }.getOrThrow()
            }
        }

    companion object {
        private val log by logger()
    }
}
