package no.nav.sosialhjelp.innsyn.app.config.security

import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Basic security resource server.
 *
 * @author Rob Winch
 * @since 5.1
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
class SecurityConfiguration(
    @Value("\${idporten.issuer}")
    private val issuer: String,
    @Value("\${idporten.audience}")
    private val audience: String,
) {
    private val log by logger()

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .cors { it.disable() }
            .authorizeExchange { authorize ->
                authorize
                    .pathMatchers("/internal/**")
                    .permitAll()
                    .anyExchange().authenticated()
            }.oauth2ResourceServer { resourceServer ->
                resourceServer
                    .jwt(Customizer.withDefaults())
            }
        return http.build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val jwtDecoder = ReactiveJwtDecoders.fromOidcIssuerLocation(issuer) as NimbusReactiveJwtDecoder

        val withAcr: OAuth2TokenValidator<Jwt> = AcrValidator()
        val audienceValidator: OAuth2TokenValidator<Jwt> = AudienceValidator(audience)
        val withAcrAndAudience: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithValidators(audienceValidator, withAcr)

        jwtDecoder.setJwtValidator(withAcrAndAudience)
        return jwtDecoder
    }
}

class AudienceValidator(private val audience: String) : OAuth2TokenValidator<Jwt> {
    var error: OAuth2Error = OAuth2Error("invalid_token", "The required audience is missing", null)

    override fun validate(jwt: Jwt): OAuth2TokenValidatorResult {
        return if (jwt.audience.contains(audience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(error)
        }
    }
}

class AcrValidator : OAuth2TokenValidator<Jwt> {
    fun error(actualAcr: String): OAuth2Error =
        OAuth2Error("invalid_token", "Wrong acr. Expected one of [Level4, idporten-loa-high], but was $actualAcr", null)

    override fun validate(jwt: Jwt): OAuth2TokenValidatorResult {
        val acr = jwt.getClaimAsString("acr")
        return if (acr in listOf("Level4", "idporten-loa-high")) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(error(acr))
        }
    }
}
