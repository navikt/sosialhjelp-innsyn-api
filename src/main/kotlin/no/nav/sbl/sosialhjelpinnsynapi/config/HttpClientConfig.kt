package no.nav.sbl.sosialhjelpinnsynapi.config

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpClientConfig {

    @Bean
    fun httpClient(): HttpClient {
        val client = HttpClient(SslContextFactory.Client())
        client.isFollowRedirects = false
        client.start()
        return client
    }

}
