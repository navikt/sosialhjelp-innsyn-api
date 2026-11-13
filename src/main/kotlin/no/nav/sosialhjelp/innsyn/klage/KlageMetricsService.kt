package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class KlageMetricsService(
    private val meterRegistry: MeterRegistry,
) {
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

    private fun result(success: Boolean) = if (success) "success" else "error"

    companion object {
        const val KLAGE_SENDT = "klage_sendt_total"
        const val ETTERSENDELSE_SENDT = "klage_ettersendelse_sendt_total"
        const val UKJENT = "ukjent"
    }
}
