package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.mdc.MDCFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.EnableWebMvc

// Denne fører til 403 (HttpStatus.FORBIDDEN) uten logg og feilmelding når den feiler.

@Configuration
@EnableWebSecurity
@EnableWebMvc
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    private val mdcFilter = MDCFilter()

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors()
        http.csrf().disable()

        addFilters(http)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "https://www.nav.no",
            "https://www-q0.nav.no",
            "https://www-q1.nav.no",
            "https://sosialhjelp-innsyn-gcp.dev.nav.no",
            "https://sosialhjelp-innsyn.labs.nais.io",
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:3002",
            "https://digisos.labs.nais.io",
            "https://digisos-gcp.dev.nav.no"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
        configuration.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "X-XSRF-TOKEN", "Authorization", "Nav-Call-Id")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun navCorsFilter(): CORSFilter {
        return CORSFilter()
    }

    private fun addFilters(http: HttpSecurity) {
        http.addFilterBefore(mdcFilter, SecurityContextPersistenceFilter::class.java)
    }
}
