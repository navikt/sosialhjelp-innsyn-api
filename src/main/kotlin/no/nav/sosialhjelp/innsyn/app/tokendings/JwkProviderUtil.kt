package no.nav.sosialhjelp.innsyn.app.tokendings

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.config.HttpClientUtil.unproxiedHttpClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull

suspend fun downloadWellKnown(url: String): WellKnown =
    withContext(Dispatchers.IO) {
        WebClient.create()
            .get()
            .uri(url)
            .retrieve()
            .awaitBodyOrNull<WellKnown>()
            ?: throw RuntimeException("Feiler under henting av well-known konfigurasjon fra $url")
    }

fun buildWebClient(
    webClientBuilder: WebClient.Builder,
    url: String,
    headers: HttpHeaders = applicationJsonHttpHeaders(),
): WebClient =
    webClientBuilder
        .baseUrl(url)
        .defaultHeaders { headers.map { it.key to it.value } }
        .clientConnector(
            ReactorClientHttpConnector(
                unproxiedHttpClient()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                    .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(60)) },
            ),
        )
        .build()

fun applicationJsonHttpHeaders(): HttpHeaders {
    val headers = HttpHeaders()
    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    return headers
}

fun applicationFormUrlencodedHeaders(): HttpHeaders {
    val headers = HttpHeaders()
    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    return headers
}
