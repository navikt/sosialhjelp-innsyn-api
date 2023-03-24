package no.nav.sosialhjelp.innsyn.kommuneinfo

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.client.mdcExchangeFilter
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil
import no.nav.sosialhjelp.innsyn.kommuneinfo.dto.KommuneDto
import no.nav.sosialhjelp.innsyn.kommuneinfo.dto.KommuneGraphqlDto
import no.nav.sosialhjelp.innsyn.kommuneinfo.dto.KommuneRequest
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KommuneClient(
    clientProperties: ClientProperties,
    webClientBuilder: WebClient.Builder,
) {

    private val webClient = webClientBuilder
        .baseUrl(clientProperties.kommuneServiceUrl)
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClientUtil.unproxiedHttpClient()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                    .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) }
            )
        )
        .filter(mdcExchangeFilter)
        .build()

    fun getKommuneDto(kommunenummer: String): KommuneDto? {
        val query = getKommuneInfoQuery().replace("[\n\r]", "")
        return webClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(KommuneRequest(query, kommunenummer))
            .retrieve()
            .bodyToMono<KommuneGraphqlDto>()
            .block()
            ?.data
            ?.kommune
    }

    private fun getKommuneInfoQuery() = getResourceAsString("/kommuneservice/hentKommuneInfo.graphql")
    private fun getResourceAsString(path: String) =
        this::class.java.getResource(path)?.readText()
            ?: throw RuntimeException("Klarte ikke å hente graphql query ressurs: $path")
}
