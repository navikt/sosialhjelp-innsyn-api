package no.nav.sbl.sosialhjelpinnsynapi.client.unleash

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.util.UnleashConfig
import no.nav.sbl.sosialhjelpinnsynapi.client.unleash.strategy.ByInstanceIdStrategy
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!mock")
@Configuration
class UnleashConfig(
        private val clientProperties: ClientProperties
) {

    @Bean
    fun unleashClient(): Unleash {
        return DefaultUnleash(
                config(),
                ByInstanceIdStrategy()
        )
    }

    private fun config() = UnleashConfig.builder()
            .appName("sosialhjelp-innsyn-api")
            .instanceId(clientProperties.unleashInstanceId)
            .unleashAPI(clientProperties.unleashUrl)
            .build()
}

@Profile("mock")
@Configuration
class UnleashMockConfig {

    @Bean
    fun unleashClient(): Unleash {
        return FakeUnleash()
    }

}