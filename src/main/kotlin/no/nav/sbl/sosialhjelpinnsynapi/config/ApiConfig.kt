package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@Configuration
@EnableWebMvc
@EnableOIDCTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger.web.ApiResourceController"])
class ApiConfig {
    @Bean
    fun corsFilter(): CORSFilter {
        return CORSFilter()
    }
}
