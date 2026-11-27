package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service

@Service
class KlageMetricsService(
    meterRegistry: MeterRegistry,
) {
    private val klageIkkeMottattGauge =
        IntegerGauge(
            meterRegistry,
            "sosialhjelp.klage.ikke.mottatt",
            "Antall klager som ikke er mottatt",
        )

    private val klageInnsendingError = meterRegistry.counter(KLAGE_INNSENDING, "result", "error")
    private val klageInnsendingSuccess = meterRegistry.counter(KLAGE_INNSENDING, "result", "success")
    private val klageOpprettetSuksess = meterRegistry.counter(KLAGE_OPPRETT, "result", "success")
    private val klageOpprettetError = meterRegistry.counter(KLAGE_OPPRETT, "result", "error")
    private val klageAvbrutt = meterRegistry.counter(KLAGE_AVBRUTT)

    fun registerKlageSent(success: Boolean) {
        (if (success) klageInnsendingSuccess else klageInnsendingError).increment()
    }

    fun registerKlageOpprettet(success: Boolean) {
        (if (success) klageOpprettetSuksess else klageOpprettetError).increment()
    }

    fun setKlagerIkkeMottattGauge(value: Int) {
        klageIkkeMottattGauge.set(value)
    }

    fun registerKlageAvbrutt() {
        klageAvbrutt.increment()
    }

    companion object {
        const val KLAGE_INNSENDING = "sosialhjelp.klage.innsending"
        const val KLAGE_OPPRETT = "sosialhjelp.klage.opprett"
        const val KLAGE_AVBRUTT = "sosialhjelp.klage.avbrutt"
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
