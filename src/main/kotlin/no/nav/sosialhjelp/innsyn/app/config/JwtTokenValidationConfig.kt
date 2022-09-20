package no.nav.sosialhjelp.innsyn.app.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc.webmvc.api.OpenApiWebMvcResource", "org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc", "org.springdoc.webmvc.ui.SwaggerConfigResource"])
class JwtTokenValidationConfig
