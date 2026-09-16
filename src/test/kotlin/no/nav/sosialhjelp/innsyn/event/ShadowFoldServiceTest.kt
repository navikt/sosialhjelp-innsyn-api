package no.nav.sosialhjelp.innsyn.event

import io.getunleash.Unleash
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.featuretoggle.HENDELSER_SHADOW
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ShadowFoldServiceTest {
    private val unleash: Unleash = mockk()
    private val meterRegistry = SimpleMeterRegistry()
    private val service = ShadowFoldService(unleash, meterRegistry)
    private val digisosSak: DigisosSak =
        mockk {
            every { fiksDigisosId } returns "fiks-id"
            every { kommunenummer } returns "0301"
            every { originalSoknadNAV } returns null
            every { digisosSoker } returns null
            every { sistEndret } returns 0L
        }

    @Test
    fun `does nothing when toggle is disabled`() =
        runTest {
            every { unleash.isEnabled(HENDELSER_SHADOW) } returns false

            service.compare(digisosSak, null, null, emptyList(), InternalDigisosSoker())

            assertThat(meterRegistry.find("hendelser_shadow_compare_total").counter()).isNull()
        }

    @Test
    fun `records match for a paper application with equivalent model`() =
        runTest {
            every { unleash.isEnabled(HENDELSER_SHADOW) } returns true
            service.compare(digisosSak, null, null, emptyList(), InternalDigisosSoker())

            assertThat(resultCount("match")).isEqualTo(1.0)
        }

    private fun resultCount(result: String): Double =
        meterRegistry
            .find("hendelser_shadow_compare_total")
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0
}
