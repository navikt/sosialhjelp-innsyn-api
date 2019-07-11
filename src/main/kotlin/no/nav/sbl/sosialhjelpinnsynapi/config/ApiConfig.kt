package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

//@EnableWebMvc
@Configuration
class ApiConfig(private val corsProperties: CorsProperties) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {

        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedOrigins(*corsProperties.allowedOrigins)
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
                .allowCredentials(true)
    }
}

@Configuration
@EnableWebSecurity
@EnableOIDCTokenValidation(ignore = ["org.springframework"])
//@Import(ApiConfig::class)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors()
//        http.csrf().disable()
    }
}

@Profile("mock")
@Order(-1)
@Configuration
//@Import(ApiConfig::class)
class WebSecurityMockConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.antMatcher("/api/v1/mock/**").csrf().disable()
        http.cors()
    }

}
