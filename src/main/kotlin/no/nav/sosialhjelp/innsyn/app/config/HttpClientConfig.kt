package no.nav.sosialhjelp.innsyn.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.netty.http.client.HttpClient

@Configuration
class HttpClientConfig {
    @Bean
    fun unproxiedHttpClient(): HttpClient = HttpClientUtil.getHttpClient()
}
