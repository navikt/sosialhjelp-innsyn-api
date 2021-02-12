package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!mock")
@Configuration
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger.web.ApiResourceController", "springfox.documentation.oas.web.OpenApiControllerWebMvc"])
class JwtTokenValidationConfig

// JwtTokenValidation er enabled så lenge appen kjører med profil != mock
