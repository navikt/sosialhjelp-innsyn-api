package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.context.SecurityContextPersistenceFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.EnableWebMvc

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
                "https://www.digisos-test.com")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
        configuration.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "X-XSRF-TOKEN", "Authorization",
                "Nav-Call-Id", "x-request-id", "x-client-trace-id", "x-b3-traceid", "x-b3-spanid", "x-b3-parentspanid",
                "x-b3-sampled", "x-b3-flags")
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

@Profile("mock")
@Order(-1)
@Configuration
class WebSecurityMockConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
        http.cors()
    }
}
