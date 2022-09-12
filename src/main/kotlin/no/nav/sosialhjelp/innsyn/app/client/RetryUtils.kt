package no.nav.sosialhjelp.innsyn.app.client

import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration

object RetryUtils {
    private const val DEFAULT_MAX_ATTEMPTS: Long = 5
    private const val DEFAULT_INITIAL_WAIT_INTERVAL_MILLIS: Long = 100

    val DEFAULT_SERVER_ERROR_FILTER: (Throwable) -> (Boolean) = { it is WebClientResponseException && it.statusCode.is5xxServerError }

    val DEFAULT_RETRY_SERVER_ERRORS = retryBackoffSpec(DEFAULT_SERVER_ERROR_FILTER)

    val PDL_RETRY: RetryBackoffSpec = retryBackoffSpec({ it is WebClientResponseException })

    fun retryBackoffSpec(
        predicate: (Throwable) -> (Boolean),
        maxAttempts: Long = DEFAULT_MAX_ATTEMPTS,
        initialWaitIntervalMillis: Long = DEFAULT_INITIAL_WAIT_INTERVAL_MILLIS
    ): RetryBackoffSpec {
        return Retry
            .backoff(maxAttempts, Duration.ofMillis(initialWaitIntervalMillis))
            .filter { predicate(it) }
    }
}
