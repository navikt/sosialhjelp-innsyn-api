package no.nav.sosialhjelp.innsyn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

// DataSource opprettes i DatabaseConfig, bare i miljøer som har database
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@EnableScheduling
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args).registerShutdownHook()
}
