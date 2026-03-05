package no.nav.sosialhjelp.innsyn.app.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds

@Configuration
class HttpClientConfig {
    @Bean
    fun httpClient(): HttpClient =
        HttpClient
            .create(defaultConnectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ofSeconds(30).toMillis().toInt())
            .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) }
            .responseTimeout(ofMinutes(2))

    @Bean
    fun fiksHttpClient(): HttpClient =
        HttpClient
            .create(fiksConnectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ofSeconds(30).toMillis().toInt())
            .doOnConnected { it.addHandlerLast(ReadTimeoutHandler(30)) }
            .responseTimeout(ofMinutes(2))
}

private val defaultConnectionProvider =
    ConnectionProvider
        .builder("default-connection-pool")
        .maxConnections(200)
        .maxIdleTime(ofMinutes(10))
        .maxLifeTime(ofMinutes(50))
        .pendingAcquireTimeout(ofSeconds(30))
        .lifo()
        .evictInBackground(ofMinutes(5))
        .metrics(true)
        .build()

private val fiksConnectionProvider =
    ConnectionProvider
        .builder("fiks-connection-pool")
        .maxConnections(300)
        .maxIdleTime(ofMinutes(10))
        .maxLifeTime(ofMinutes(50))
        .pendingAcquireTimeout(ofSeconds(30))
        .lifo()
        .evictInBackground(ofMinutes(5))
        .metrics(true)
        .build()
