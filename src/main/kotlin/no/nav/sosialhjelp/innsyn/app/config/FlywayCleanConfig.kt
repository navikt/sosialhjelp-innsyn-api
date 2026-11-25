package no.nav.sosialhjelp.innsyn.app.config

import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Sletter alle databaseobjekter og migrerer på nytt ved hver oppstart med mock-alt (mock-miljøet og lokalt),
 * der databasen ikke har data som må tas vare på.
 */
@Configuration
@Profile("mock-alt")
class FlywayCleanConfig {
    @Bean
    fun flywayCleanEnabler() = FlywayConfigurationCustomizer { it.cleanDisabled(false) }

    @Bean
    fun cleanMigrateStrategy() =
        FlywayMigrationStrategy { flyway ->
            log.warn("Profil mock-alt: sletter databasen før migrering")
            flyway.clean()
            flyway.migrate()
        }

    companion object {
        private val log by logger()
    }
}
