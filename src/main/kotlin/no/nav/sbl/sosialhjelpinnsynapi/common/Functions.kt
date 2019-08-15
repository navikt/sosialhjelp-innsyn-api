package no.nav.sbl.sosialhjelpinnsynapi.common

import io.ktor.client.features.ClientRequestException
import kotlinx.coroutines.delay
import java.util.*
import kotlin.reflect.KClass

private class Common

internal fun getStringFromResource(path: String) =
    Common::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }

internal fun randomUuid() = UUID.randomUUID().toString()
internal fun decodeBase64(s: String): ByteArray = Base64.getDecoder().decode(s)
internal fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

internal suspend fun <T> retry(
        callName: String,
        attempts: Int = 10,
        initialDelay: Long = 100L,
        maxDelay: Long = 1000L,
        vararg illegalExceptions: KClass<out Throwable> = arrayOf(),
        block: suspend () -> T
): T {
    //Metrics.networkCallFailuresCounter.labels(callName)
    var currentDelay = initialDelay
    repeat(attempts - 1) { attempt ->
        try {
            return timed(callName) { block() }
        } catch (e: Throwable) {
        //    Metrics.networkCallFailuresCounter.labels(callName).inc()
            // Any exception deemed to be client/user error should be propagated immediately rather than retried
            // Equivalently for HTTP client calls resulting in status codes between 400 and 499.
            if (illegalExceptions.any { it.isInstance(e) } || (e is ClientRequestException)) {
                countAndRethrowError(e, callName) {
               //     logger.warn { "$callName: Propagating illegal exception - ${e.message}" }
                }
            }
          //  logger.warn { "$callName: Attempt ${attempt + 1} of $attempts failed - retrying in $currentDelay ms - ${e.message}" }
        }
        delay(currentDelay)
        currentDelay = (currentDelay * 2.0).toLong().coerceAtMost(maxDelay)
    }
    return try {
        timed(callName) { block() }
    } catch (e: Throwable) {
        countAndRethrowError(e, callName) {
       //     logger.error { "$callName: Final retry attempt #$attempts failed - ${e.message}" }
        }
    }
}

private fun countAndRethrowError(e: Throwable, callName: String, block: () -> Any?): Nothing {
   // Metrics.networkCallFailuresCounter.labels(callName).inc()
    block()
    throw e
}

internal suspend inline fun <T> timed(callName: String, crossinline block: suspend () -> T) =
 //  Metrics.networkCallSummary.labels(callName).startTimer().use {
        block()
  //  }

internal inline fun <reified T : Enum<T>> ignoreCaseValueOf(name: String, defaultValue: T? = null): T =
    enumValues<T>().firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: defaultValue
        ?: throw InvalidInputException(
            "Invalid type '$name' for enum ${T::class.java.name}, valid types: ${enumValues<T>()
                .joinToString(prefix = "[", postfix = "]")}"
        )
