package no.nav.sosialhjelp.innsyn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
@EnableWebFlux
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args).registerShutdownHook()
}
