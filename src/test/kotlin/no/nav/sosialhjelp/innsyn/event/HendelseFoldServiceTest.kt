package no.nav.sosialhjelp.innsyn.event

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HendelseFoldServiceTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val service = HendelseFoldService(meterRegistry)
    private val digisosSak: DigisosSak =
        mockk {
            every { fiksDigisosId } returns "fiks-id"
            every { kommunenummer } returns "0301"
            every { originalSoknadNAV } returns null
            every { digisosSoker } returns null
            every { sistEndret } returns 0L
        }

    @Test
    fun `records match for a paper application with equivalent model`() =
        runTest {
            service.fold(digisosSak, null, null, emptyList(), InternalDigisosSoker())

            assertThat(resultCount("match")).isEqualTo(1.0)
        }

    private fun resultCount(result: String): Double =
        meterRegistry
            .find("hendelser_fold_total")
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0
}
