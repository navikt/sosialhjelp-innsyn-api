package no.nav.sosialhjelp.innsyn.app.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Erstatter DataSourceAutoConfiguration, som ellers feiler ved oppstart i miljøer uten database.
 * JdbcClient og transaksjonshåndtering kommer fra Spring Boot når denne DataSource-en finnes.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource", name = ["url"])
@EnableConfigurationProperties(DataSourceProperties::class)
class DatabaseConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    fun dataSource(properties: DataSourceProperties): HikariDataSource =
        properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
}
