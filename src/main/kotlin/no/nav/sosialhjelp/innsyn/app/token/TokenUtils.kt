package no.nav.sosialhjelp.innsyn.app.token

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object TokenUtils {
    suspend fun getUserIdFromToken(): String = getUserIdFromTokenOrNull() ?: error("No userId for token")

    suspend fun getUserIdFromTokenOrNull(): String? =
        when (val authenticationToken = getAuthenticationToken()) {
            is JwtAuthenticationToken -> {
                val jwt: Jwt = authenticationToken.credentials as Jwt
                jwt.getClaim("pid") ?: jwt.subject
            }

            is UsernamePasswordAuthenticationToken -> {
                authenticationToken.name
            }
            is TestingAuthenticationToken -> {
                authenticationToken.credentials?.toString()
            }
            else -> TODO()
        }

    suspend fun getToken(): Token = getTokenOrNull() ?: error("No token in request context")

    private suspend fun getTokenOrNull(): Token? =
        when (val authenticationToken = getAuthenticationToken()) {
            is JwtAuthenticationToken -> {
                val creds = authenticationToken.credentials as OAuth2Token
                creds.tokenValue?.let { Token(it) }
            }

            is UsernamePasswordAuthenticationToken -> {
                authenticationToken.credentials?.toString()?.let { Token(it) }
            }
            is TestingAuthenticationToken -> {
                authenticationToken.credentials?.toString()?.let { Token(it) }
            }
            else -> TODO()
        }

    private suspend fun getAuthenticationToken(): Authentication? =
        ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()?.authentication
}

@JvmInline
value class Token(
    val value: String,
) {
    fun withBearer() = "Bearer $value"
}
