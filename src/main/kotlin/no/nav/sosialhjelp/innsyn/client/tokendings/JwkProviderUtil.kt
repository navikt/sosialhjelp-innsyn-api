package no.nav.sosialhjelp.innsyn.client.tokendings

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

fun downloadWellKnown(url: String): WellKnown =
    WebClient.create()
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(WellKnown::class.java)
        .block()
        ?: throw RuntimeException("Feiler under henting av well-known konfigurasjon fra $url")

fun buildWebClient(nonProxiedWebClientBuilder: WebClient.Builder, url: String, headers: HttpHeaders = applicationJsonHttpHeaders()): WebClient =
    nonProxiedWebClientBuilder
        .baseUrl(url)
        .defaultHeaders { headers.map { it.key to it.value } }
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.newConnection()
                    .resolver(DefaultAddressResolverGroup.INSTANCE)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                    .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(60)) }
            )
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
