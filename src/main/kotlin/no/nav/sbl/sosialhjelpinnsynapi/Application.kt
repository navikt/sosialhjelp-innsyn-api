package no.nav.sbl.sosialhjelpinnsynapi

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableOIDCTokenValidation
class Application

    fun main(args: Array<String>) {
        runApplication<Application>(*args)
    }