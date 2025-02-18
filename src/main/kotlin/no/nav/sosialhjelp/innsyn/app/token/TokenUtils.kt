package no.nav.sosialhjelp.innsyn.app.token

import com.auth0.jwt.JWT
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import no.nav.sosialhjelp.innsyn.utils.logger

object TokenUtils {
    private val log by logger()

    suspend fun getUserIdFromToken(): String {
        return getUserIdFromTokenOrNull() ?: error("No userId for token")
    }

    suspend fun getUserIdFromTokenOrNull(): String? {
        val jwt = getTokenOrNull()?.let { JWT.decode(it) }
        return jwt?.getClaim("pid")?.asString() ?: jwt?.subject
    }

    suspend fun getToken(): String {
        return getTokenOrNull() ?: error("No token in request context")
    }

    private suspend fun getTokenOrNull(): String? {
        return currentCoroutineContext()[ReactorContext]?.context?.getOrDefault<String?>("authToken", null)
    }
}
