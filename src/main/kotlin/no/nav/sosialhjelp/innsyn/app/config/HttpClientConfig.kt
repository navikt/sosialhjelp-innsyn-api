package no.nav.sosialhjelp.innsyn.app.config

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient

@Configuration
class HttpClientConfig {
    @Bean
    fun httpClient(): HttpClient =
        HttpClient
            .newConnection()
            .resolver(DefaultAddressResolverGroup.INSTANCE)
}
