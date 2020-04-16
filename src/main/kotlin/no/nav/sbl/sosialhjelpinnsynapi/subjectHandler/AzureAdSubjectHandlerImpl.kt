package no.nav.sbl.sosialhjelpinnsynapi.subjectHandler

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AzureAdSubjectHandlerImpl(val tokenValidationContextHolder: TokenValidationContextHolder) : SubjectHandlerInterface {
    private val ISSUER = "azuread"

    private val tokenValidationContext: TokenValidationContext
        get() {
            val tokenValidationContext = tokenValidationContextHolder.tokenValidationContext
            if (tokenValidationContext == null) {
                logger.error("Could not find TokenValidationContext. Possibly no token in request and request was not captured by JwtToken-validation filters.")
                throw JwtTokenValidatorException("Could not find TokenValidationContext. Possibly no token in request.")
            }
            return tokenValidationContext
        }

    override fun getUserIdFromToken(): String {
        return tokenValidationContext.getClaims(ISSUER).subject
    }

    override fun getToken(): String {
        return tokenValidationContext.getJwtToken(ISSUER).tokenAsString
    }

    override fun getConsumerId(): String {
        return System.getProperty("consumerid")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AzureAdSubjectHandlerImpl::class.java)
    }
}