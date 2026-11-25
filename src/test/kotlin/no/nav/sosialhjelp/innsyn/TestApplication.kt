package no.nav.sosialhjelp.innsyn

import org.springframework.boot.fromApplication

fun main(args: Array<String>) {
    fromApplication<Application>().run(*args)
}
