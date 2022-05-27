package no.nav.sosialhjelp.innsyn.common.subjecthandler

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

interface SubjectHandler {
    fun getConsumerId(): String
    fun getUserIdFromToken(): String
    fun getToken(): String
    fun getTokenOld(): String
    fun getClientId(): String
}

@Component
class AzureAdSubjectHandlerImpl(
    private val tokenValidationContextHolder: TokenValidationContextHolder
) : SubjectHandler {

    private val tokenValidationContext: TokenValidationContext
        get() {
            val tokenValidationContext = tokenValidationContextHolder.tokenValidationContext
            if (tokenValidationContext == null) {
                log.error("Could not find TokenValidationContext. Possibly no token in request and request was not captured by JwtToken-validation filters.")
                throw JwtTokenValidatorException("Could not find TokenValidationContext. Possibly no token in request.")
            }
            return tokenValidationContext
        }

    override fun getUserIdFromToken(): String {
        val pid: String? = tokenValidationContext.getClaims(ISSUER).getStringClaim(PID)
        val sub: String? = tokenValidationContext.getClaims(ISSUER).subject
        return pid ?: sub ?: throw RuntimeException("Could not find any userId for token in pid or sub claim")
    }

    override fun getToken(): String {
        return tokenValidationContext.getJwtToken(ISSUER).tokenAsString
    }

    override fun getTokenOld(): String {
        return tokenValidationContext.getJwtToken(ISSUER_OLD).tokenAsString
    }

    override fun getClientId(): String {
        return tokenValidationContext.getClaims(ISSUER).getStringClaim(CLIENT_ID)
    }

    override fun getConsumerId(): String {
        return System.getProperty("consumerid")
    }

    companion object {
        private const val ISSUER = "selvbetjening"
        private const val ISSUER_OLD = "selvbetjening-old"
        private const val PID = "pid"
        private const val CLIENT_ID = "client_id"
        private val log by logger()
    }
}

class StaticSubjectHandlerImpl : SubjectHandler {
    private val DEFAULT_USER = "11111111111"
    private val DEFAULT_TOKEN = "token"
    private var user = DEFAULT_USER
    private var token = DEFAULT_TOKEN

    override fun getUserIdFromToken(): String {
        return this.user
    }

    override fun getToken(): String {
        return this.token
    }

    override fun getTokenOld(): String {
        return this.token
    }

    override fun getConsumerId(): String {
        return "StaticConsumerId"
    }

    override fun getClientId(): String {
        return "clientId"
    }

    fun setUser(user: String) {
        this.user = user
    }

    fun setFakeToken(fakeToken: String) {
        this.token = fakeToken
    }

    fun reset() {
        this.user = DEFAULT_USER
        this.token = DEFAULT_TOKEN
    }
}
