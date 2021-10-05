package no.nav.sosialhjelp.innsyn.client.unleash

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.util.UnleashConfig
import no.nav.sosialhjelp.innsyn.client.unleash.strategy.ByInstanceIdStrategy
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!local")
@Configuration
class UnleashConfig(
    private val clientProperties: ClientProperties
) {

    @Bean
    fun unleashClient(): Unleash {
        return DefaultUnleash(
            config(),
            ByInstanceIdStrategy(clientProperties.unleashInstanceId)
        )
    }

    private fun config() = UnleashConfig.builder()
        .appName("sosialhjelp-innsyn-api")
        .instanceId(clientProperties.unleashInstanceId)
        .unleashAPI(clientProperties.unleashUrl)
        .build()
}

@Profile("local")
@Configuration
class UnleashMockConfig {

    @Bean
    fun unleashClient(): Unleash {
        return FakeUnleash()
    }
}
