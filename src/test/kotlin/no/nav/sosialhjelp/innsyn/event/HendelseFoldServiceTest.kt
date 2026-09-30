package no.nav.sosialhjelp.innsyn.event

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HendelseFoldServiceTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val digisosSak: DigisosSak =
        mockk {
            every { fiksDigisosId } returns "fiks-id"
            every { kommunenummer } returns "0301"
            every { originalSoknadNAV } returns null
            every { digisosSoker } returns null
            every { sistEndret } returns 0L
        }

    private fun TestScope.service() = HendelseFoldService(meterRegistry, StandardTestDispatcher(testScheduler))

    @Test
    fun `does not fetch vedlegg for a paper application`() =
        runTest {
            var fetched = false
            service()
                .launchFold(digisosSak, null, null, InternalDigisosSoker()) {
                    fetched = true
                    emptyList()
                }.join()

            assertThat(fetched).isFalse()
            assertThat(resultCount("match")).isEqualTo(1.0)
        }

    @Test
    fun `records error when vedlegg fetch fails`() =
        runTest {
            service().launchFold(recentDigitalDigisosSak(), null, null, InternalDigisosSoker()) { error("Fiks failed") }.join()

            assertThat(resultCount("error")).isEqualTo(1.0)
            assertThat(resultCount("match")).isEqualTo(0.0)
        }

    @Test
    fun `records timeout when vedlegg fetch is slow`() =
        runTest {
            service().launchFold(recentDigitalDigisosSak(), null, null, InternalDigisosSoker()) { awaitCancellation() }.join()

            assertThat(resultCount("timeout")).isEqualTo(1.0)
        }

    @Test
    fun `propagates caller context to the background fold`() =
        runTest {
            var observedName: String? = null
            withContext(CoroutineName("caller")) {
                service()
                    .launchFold(recentDigitalDigisosSak(), null, null, InternalDigisosSoker()) {
                        observedName = currentCoroutineContext()[CoroutineName]?.name
                        emptyList()
                    }
            }.join()

            assertThat(observedName).isEqualTo("caller")
        }

    @Test
    fun `fold is not cancelled when the caller completes`() =
        runTest {
            val job =
                withContext(CoroutineName("request")) {
                    service().launchFold(digisosSak, null, null, InternalDigisosSoker()) { emptyList() }
                }

            job.join()
            assertThat(job.isCancelled).isFalse()
            assertThat(resultCount("match")).isEqualTo(1.0)
        }

    private fun resultCount(result: String): Double =
        meterRegistry
            .find("hendelser_fold_total")
            .tag("result", result)
            .counter()
            ?.count() ?: 0.0

    private fun recentDigitalDigisosSak(): DigisosSak =
        mockk {
            every { fiksDigisosId } returns "fiks-id"
            every { kommunenummer } returns "0301"
            every { originalSoknadNAV } returns
                OriginalSoknadNAV(
                    navEksternRefId = "ref",
                    metadata = "metadata",
                    vedleggMetadata = "vedlegg-metadata",
                    soknadDokument = DokumentInfo("soknad.pdf", "soknad-dokument", 0),
                    vedlegg = emptyList(),
                    timestampSendt = System.currentTimeMillis(),
                )
            every { digisosSoker } returns null
            every { sistEndret } returns 0L
        }
}
