package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@Configuration
@EnableWebSecurity
@EnableWebMvc
@EnableOIDCTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger.web.ApiResourceController"])
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors()
        http.csrf().disable()
    }

    @Bean
    fun navCorsFilter(): CORSFilter {
        return CORSFilter()
    }
}
