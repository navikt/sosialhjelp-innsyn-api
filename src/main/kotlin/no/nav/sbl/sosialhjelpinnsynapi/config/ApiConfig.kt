package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

private val ALLOWED_ORIGINS = arrayOf(
        "http://localhost:3000",
        "https://sosialhjelp-innsyn-q0.nais.oera-q.local",
        "https://sosialhjelp-innsyn-q1.nais.oera-q.local",
        "https://veivisersosialhjelp-q0.nais.oera-q.local",
        "https://veivisersosialhjelp-q1.nais.oera-q.local"
// TODO: legg til origin for prod?
)

class ApiConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedOrigins(*ALLOWED_ORIGINS)
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
                .allowCredentials(true)
    }
}

@Configuration
@EnableWebSecurity
@EnableOIDCTokenValidation(ignore = ["org.springframework"])
@Import(ApiConfig::class)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
//        http.csrf().disable()
    }

}