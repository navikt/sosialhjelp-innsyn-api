package no.nav.sosialhjelp.innsyn.app.token

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext

object TokenUtils {
    suspend fun getUserIdFromToken(): String {
        return getUserIdFromTokenOrNull() ?: error("No userId for token")
    }

    suspend fun getUserIdFromTokenOrNull(): String? {
        return ""
    }

    suspend fun getToken(): String {
        return getTokenOrNull() ?: error("No token in request context")
    }

    private suspend fun getTokenOrNull(): String? {
        return currentCoroutineContext()[ReactorContext]?.context?.getOrDefault<String?>("authToken", null)
    }
}
