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
    suspend fun getUserIdFromToken(): String {
        return getUserIdFromTokenOrNull() ?: error("No userId for token")
    }

    suspend fun getUserIdFromTokenOrNull(): String? {
        return when (val authenticationToken = getAuthenticationToken()) {
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
    }

    suspend fun getToken(): String {
        return getTokenOrNull() ?: error("No token in request context")
    }

    private suspend fun getTokenOrNull(): String? {
        return when (val authenticationToken = getAuthenticationToken()) {
            is JwtAuthenticationToken -> {
                val creds = authenticationToken.credentials as OAuth2Token
                creds.tokenValue
            }

            is UsernamePasswordAuthenticationToken -> {
                authenticationToken.credentials?.toString()
            }
            is TestingAuthenticationToken -> {
                authenticationToken.credentials?.toString()
            }
            else -> TODO()
        }
    }

    private suspend fun getAuthenticationToken(): Authentication? =
        ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()?.authentication
}
