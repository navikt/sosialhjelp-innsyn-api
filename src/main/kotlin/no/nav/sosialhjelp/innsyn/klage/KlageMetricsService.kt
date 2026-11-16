package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.stereotype.Component

@Service
@Component
class KlageMetricsService(
    private val meterRegistry: MeterRegistry,
) {
    private val klageIkkeMottattGauge =
        IntegerGauge(meterRegistry, "klage.ikke.mottatt", "Antall klager som ikke er mottatt")

    fun registerSent() {
        meterRegistry.counter("klage.send.success").increment()
    fun registerKlageSent(
        kommunenummer: String?,
        success: Boolean,
    ) {
        meterRegistry
            .counter(
                KLAGE_SENDT,
                "result",
                result(success),
                "kommunenummer",
                kommunenummer?.takeIf { it.isNotBlank() } ?: UKJENT,
            ).increment()
    }

    fun registerEttersendelseSent(success: Boolean) {
        meterRegistry.counter(ETTERSENDELSE_SENDT, "result", result(success)).increment()
    }

    fun registerSendError() {
        meterRegistry.counter("klage.send.error").increment()
    }

    fun setKlagerIkkeMottattGauge(value: Int) {
        klageIkkeMottattGauge.set(value)
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
    private fun result(success: Boolean) = if (success) "success" else "error"

    companion object {
        const val KLAGE_SENDT = "klage_sendt_total"
        const val ETTERSENDELSE_SENDT = "klage_ettersendelse_sendt_total"
        const val UKJENT = "ukjent"
    }
}
