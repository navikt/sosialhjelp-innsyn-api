package no.nav.sosialhjelp.innsyn.app.config

import io.netty.channel.ChannelOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds

@Configuration
class HttpClientConfig {
    @Bean
    fun httpClient(): HttpClient {
        val provider =
            ConnectionProvider
                .builder("fixed")
                .maxConnections(300)
                .maxIdleTime(ofMinutes(10))
                .maxLifeTime(ofMinutes(50))
                .pendingAcquireTimeout(ofSeconds(30))
                .lifo()
                .evictInBackground(ofMinutes(5))
                .build()
        return HttpClient
            .create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ofSeconds(30).toMillis().toInt())
            .responseTimeout(ofMinutes(2))
    }
}
