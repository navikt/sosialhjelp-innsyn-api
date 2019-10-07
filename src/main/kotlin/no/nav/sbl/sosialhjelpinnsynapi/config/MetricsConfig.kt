package no.nav.sbl.sosialhjelpinnsynapi.config

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class CommonConfig {

    /**
     * Make sure spring uses the defaultRegistry
     */
    @Bean
    fun collectorRegistry(): CollectorRegistry {
        DefaultExports.initialize()
        return CollectorRegistry.defaultRegistry
    }
}