package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

@Service
class KlageMetricsService(
    private val meterRegistry: MeterRegistry,
) {
    private val klageIkkeMottattGauge =
        IntegerGauge(meterRegistry, "sosialhjelp.klage.ikke.mottatt", "Antall klager som ikke er mottatt")

    private val klageSendtSuccess = meterRegistry.counter(KLAGE_SENDT, "result", "success")
    private val klageSendtError = meterRegistry.counter(KLAGE_SENDT, "result", "error")

    fun registerKlageSent(success: Boolean) {
        (if (success) klageSendtSuccess else klageSendtError).increment()
    }

    fun setKlagerIkkeMottattGauge(value: Int) {
        klageIkkeMottattGauge.set(value)
    }

    companion object {
        const val KLAGE_SENDT = "sosialhjelp.klage.innsending"
    }
}

class IntegerGauge(
    meterRegistry: MeterRegistry,
    name: String,
    description: String?,
) {
    private var currentValue: Int? = null

    init {
        Gauge
            .builder(name) { currentValue?.toDouble() ?: Double.NaN }
            .description(description)
            .register(meterRegistry)
    }

    fun set(value: Int) {
        currentValue = value
    }
}
