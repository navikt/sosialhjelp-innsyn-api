package no.nav.sosialhjelp.innsyn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args).registerShutdownHook()
}
