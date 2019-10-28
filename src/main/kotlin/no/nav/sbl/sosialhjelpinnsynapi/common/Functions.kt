package no.nav.sbl.sosialhjelpinnsynapi.common

import io.ktor.client.features.ClientRequestException
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

internal suspend fun <T> retry(
        attempts: Int = 10,
        initialDelay: Long = 100L,
        maxDelay: Long = 1000L,
        vararg illegalExceptions: KClass<out Throwable> = arrayOf(),
        block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(attempts - 1) {
        try {
            return timed { block() }
        } catch (e: Throwable) {
            if (illegalExceptions.any { it.isInstance(e) } || (e is ClientRequestException)) {
                countAndRethrowError(e) {
                }
            }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * 2.0).toLong().coerceAtMost(maxDelay)
    }
    return try {
        timed { block() }
    } catch (e: Throwable) {
        countAndRethrowError(e) {
        }
    }
}

private fun countAndRethrowError(e: Throwable, block: () -> Any?): Nothing {
    block()
    throw e
}

internal suspend inline fun <T> timed(crossinline block: suspend () -> T) =
        block()

