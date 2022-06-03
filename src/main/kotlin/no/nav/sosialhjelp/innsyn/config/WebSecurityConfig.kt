package no.nav.sosialhjelp.innsyn.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

// Denne fører til 403 (HttpStatus.FORBIDDEN) uten logg og feilmelding når den feiler.

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors()
        http.csrf().disable()

        return http.build()
    }
}
