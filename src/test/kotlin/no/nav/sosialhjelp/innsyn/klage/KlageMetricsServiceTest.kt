package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KlageMetricsServiceTest {
    private val registry = SimpleMeterRegistry()
    private val service = KlageMetricsService(registry)

    @Test
    fun `initialiserer success og error med 0`() {
        assertThat(count("success")).isEqualTo(0.0)
        assertThat(count("error")).isEqualTo(0.0)
    }

    @Test
    fun `registrerer sendt klage med result`() {
        service.registerKlageSent(success = true)
        service.registerKlageSent(success = false)

        assertThat(count("success")).isEqualTo(1.0)
        assertThat(count("error")).isEqualTo(1.0)
    }

    private fun count(result: String) = registry.get(KlageMetricsService.KLAGE_SENDT).tag("result", result).counter().count()
}
