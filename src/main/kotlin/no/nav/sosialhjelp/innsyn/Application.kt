package no.nav.sosialhjelp.innsyn

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["no.nav"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args).registerShutdownHook()
}
