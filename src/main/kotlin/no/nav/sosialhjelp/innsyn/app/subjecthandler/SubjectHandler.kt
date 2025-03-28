package no.nav.sosialhjelp.innsyn.app.subjecthandler

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.SELVBETJENING
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.stereotype.Component

interface SubjectHandler {
    fun getUserIdFromToken(): String

    fun getUserIdFromTokenOrNull(): String?

    fun getToken(): String

    fun getClientId(): String
}

@Component
class AzureAdSubjectHandlerImpl(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) : SubjectHandler {
    private val log by logger()

    private val tokenValidationContext: TokenValidationContext
        get() = tokenValidationContextHolder.getTokenValidationContext()

    override fun getUserIdFromToken(): String =
        getUserIdFromTokenOrNull() ?: throw RuntimeException("Could not find any userId for token in pid or sub claim")

    override fun getUserIdFromTokenOrNull(): String? {
        if (!tokenValidationContext.hasTokenFor(SELVBETJENING)) {
            return null
        }
        val pid: String? = tokenValidationContext.getClaims(SELVBETJENING).getStringClaim(PID)
        val sub: String? = tokenValidationContext.getClaims(SELVBETJENING).subject
        return pid ?: sub
    }

    override fun getToken(): String =
        tokenValidationContext.getJwtToken(SELVBETJENING)?.encodedToken ?: run {
            log.error(
                "Could not find TokenValidationContext. " +
                    "Possibly no token in request and request was not captured by JwtToken-validation filters.",
            )
            throw JwtTokenValidatorException("Could not find TokenValidationContext. Possibly no token in request.")
        }

    override fun getClientId(): String = tokenValidationContext.getClaims(SELVBETJENING).getStringClaim(CLIENT_ID) ?: DEFAULT_CLIENT_ID

    companion object {
        private const val PID = "pid"
        private const val CLIENT_ID = "client_id"
        private const val DEFAULT_CLIENT_ID = "clientId"
    }
}

class StaticSubjectHandlerImpl : SubjectHandler {
    companion object {
        private const val DEFAULT_USER = "11111111111"
        private const val DEFAULT_TOKEN = "token"
    }

    private var user = DEFAULT_USER
    private var token = DEFAULT_TOKEN

    override fun getUserIdFromToken(): String = this.user

    override fun getUserIdFromTokenOrNull(): String = this.user

    override fun getToken(): String = this.token

    override fun getClientId(): String = "clientId"

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
