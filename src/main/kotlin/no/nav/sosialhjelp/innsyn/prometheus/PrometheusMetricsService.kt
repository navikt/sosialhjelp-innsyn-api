package no.nav.sosialhjelp.innsyn.prometheus

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

abstract class PrometheusMetricsService(private val meterRegistry: MeterRegistry) {

    protected fun createCounter(name: String): Counter =
        Counter.builder(name)
            .register(meterRegistry)
            .also { it.increment(0.0) }
}