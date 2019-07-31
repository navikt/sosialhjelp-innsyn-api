package no.nav.sbl.sosialhjelpinnsynapi.health.selftest

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.vavr.control.Try
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

abstract class AbstractDependencyCheck(
        protected val type: DependencyType,
        private val name: String,
        protected val address: String,
        private val importance: Importance) {

    private val circuitBreakerRegistry: CircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
    private val executor = Executors.newSingleThreadExecutor()
    private val timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(2800))
            .cancelRunningFuture(true)
            .build()
    private val timeLimiter = TimeLimiter.of(timeLimiterConfig)
    private val dependency_status = AtomicInteger()

    protected abstract fun doCheck()

    fun check(): Try<DependencyCheckResult> {
        val futureSupplier: Supplier<Future<DependencyCheckResult>> = Supplier { executor.submit(getCheckCallable()) }
        val timeRestrictedCall = TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier)
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(name)
        val chainedCallable = CircuitBreaker.decorateCallable(circuitBreaker, timeRestrictedCall)
        return Try.ofCallable(chainedCallable)
                .onSuccess { dependency_status.set(1) }
                .onFailure { dependency_status.set(0) }
                //                    log.error("Call to dependency={} with type={} at url={} timed out or circuitbreaker was tripped.", name, type, address, throwable)

                .recover { throwable ->
                    DependencyCheckResult(
                            endpoint = name,
                            result = if (importance == Importance.CRITICAL) Result.ERROR else Result.WARNING,
                            address = address,
                            errorMessage = "Call to dependency=$name timed out or circuitbreaker tripped. Errormessage=${getErrorMessageFromThrowable(throwable)}",
                            type = type,
                            importance = importance,
                            responseTime = null,
                            throwable = throwable)
                }
    }

    private fun getCheckCallable(): Callable<DependencyCheckResult> {
        return Callable {
            val start = Instant.now()
            doCheck()
            val end = Instant.now()
            val responseTime = Duration.between(start, end).toMillis()

            DependencyCheckResult(
                    type = type,
                    endpoint = name,
                    importance = importance,
                    address = address,
                    result = Result.OK,
                    errorMessage = null,
                    responseTime = "${responseTime.toString()}ms",
                    throwable = null
            )
        }
    }

    private fun getErrorMessageFromThrowable(e: Throwable): String? {
        if (e is TimeoutException) {
            return "Call to dependency timed out by circuitbreaker"
        }
        return if (e.cause == null) e.message else e.cause!!.message
    }

}