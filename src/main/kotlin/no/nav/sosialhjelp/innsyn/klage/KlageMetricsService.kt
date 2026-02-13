package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.MeterRegistry

class KlageMetricsService(
    private val meterRegistry: MeterRegistry,
) {
    fun registerSent() {
        meterRegistry.counter("klage_sent").increment()
    }

    fun registerSendError() {
        meterRegistry.counter("klage_send_error").increment()
    }
}
