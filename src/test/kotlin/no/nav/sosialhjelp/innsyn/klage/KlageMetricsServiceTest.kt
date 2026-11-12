package no.nav.sosialhjelp.innsyn.klage

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KlageMetricsServiceTest {
    private val registry = SimpleMeterRegistry()
    private val service = KlageMetricsService(registry)

    @Test
    fun `registrerer sendt klage med result og kommunenummer`() {
        service.registerKlageSent("0301", success = true)
        service.registerKlageSent("0301", success = false)

        assertThat(count(KlageMetricsService.KLAGE_SENDT, "result", "success", "kommunenummer", "0301")).isEqualTo(1.0)
        assertThat(count(KlageMetricsService.KLAGE_SENDT, "result", "error", "kommunenummer", "0301")).isEqualTo(1.0)
    }

    @Test
    fun `manglende kommunenummer gir ukjent`() {
        service.registerKlageSent(null, success = false)
        service.registerKlageSent("", success = false)

        assertThat(count(KlageMetricsService.KLAGE_SENDT, "result", "error", "kommunenummer", "ukjent")).isEqualTo(2.0)
    }

    @Test
    fun `registrerer ettersendelse med result`() {
        service.registerEttersendelseSent(success = true)
        service.registerEttersendelseSent(success = false)
        service.registerEttersendelseSent(success = false)

        assertThat(count(KlageMetricsService.ETTERSENDELSE_SENDT, "result", "success")).isEqualTo(1.0)
        assertThat(count(KlageMetricsService.ETTERSENDELSE_SENDT, "result", "error")).isEqualTo(2.0)
    }

    private fun count(
        name: String,
        vararg tags: String,
    ) = registry.get(name).tags(*tags).counter().count()
}
