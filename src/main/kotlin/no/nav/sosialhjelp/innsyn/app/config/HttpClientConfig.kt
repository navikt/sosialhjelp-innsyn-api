package no.nav.sosialhjelp.innsyn.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Configuration
class HttpClientConfig {
    @Bean
    fun unproxiedHttpClient(): HttpClient {
        val provider = ConnectionProvider.builder("fixed")
            .maxConnections(500)
            .maxIdleTime(20.seconds.toJavaDuration())
            .maxLifeTime(60.seconds.toJavaDuration())
            .pendingAcquireTimeout(60.seconds.toJavaDuration())
            .evictInBackground(120.seconds.toJavaDuration()).build()
        return HttpClient.create(provider)
    }
}
