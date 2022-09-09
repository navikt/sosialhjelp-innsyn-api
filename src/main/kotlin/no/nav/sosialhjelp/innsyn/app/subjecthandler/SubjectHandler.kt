package no.nav.sosialhjelp.innsyn.app.subjecthandler

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

interface SubjectHandler {
    fun getUserIdFromToken(): String
    fun getToken(): String
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
        val pid: String? = tokenValidationContext.getClaims(SELVBETJENING).getStringClaim(PID)
        val sub: String? = tokenValidationContext.getClaims(SELVBETJENING).subject
        return pid ?: sub ?: throw RuntimeException("Could not find any userId for token in pid or sub claim")
    }

    override fun getToken(): String {
        return tokenValidationContext.getJwtToken(SELVBETJENING).tokenAsString
    }

    companion object {
        private const val PID = "pid"
        private val log by logger()
    }
}

class StaticSubjectHandlerImpl : SubjectHandler {

    companion object {
        private const val DEFAULT_USER = "11111111111"
        private const val DEFAULT_TOKEN = "token"
    }

    private var user = DEFAULT_USER
    private var token = DEFAULT_TOKEN

    override fun getUserIdFromToken(): String {
        return this.user
    }

    override fun getToken(): String {
        return this.token
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
