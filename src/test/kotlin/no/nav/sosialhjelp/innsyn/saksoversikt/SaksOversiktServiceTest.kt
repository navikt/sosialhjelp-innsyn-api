package no.nav.sosialhjelp.innsyn.saksoversikt

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.time.Duration.Companion.seconds

internal class SaksOversiktServiceTest {
    private val fiksService: FiksService = mockk()

    private val saksOversiktService = SaksOversiktService(fiksService)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    @BeforeEach
    internal fun setUp() {
        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 2000L
        every { digisosSak1.kommunenummer } returns "0301"
        every { digisosSak1.originalSoknadNAV } returns
            mockk {
                every { navEksternRefId } returns "123"
                every { timestampSendt } returns 1000L
            }
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.kommunenummer } returns "0301"
        every { digisosSak2.originalSoknadNAV } returns
            mockk {
                every { navEksternRefId } returns "123"
                every { timestampSendt } returns 500L
            }
        every { digisosSak2.digisosSoker } returns
            mockk {
                every { timestampSistOppdatert } returns System.currentTimeMillis()
            }
    }

    @Test
    internal fun `skal mappe fra DigisosSak til SaksListeResponse`() =
        runTest(timeout = 5.seconds) {
            coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak1, digisosSak2)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(2)
            assertThat(alleSaker[0].fiksDigisosId).isEqualTo("456")
            assertThat(alleSaker[0].soknadTittel).isEqualTo("saker.default_tittel")
            assertThat(alleSaker[1].fiksDigisosId).isEqualTo("123")
            assertThat(alleSaker[1].soknadTittel).isEqualTo("saker.default_tittel")
        }

    @Test
    internal fun `Skal ta høyeste sistOppdatert`() =
        runTest(timeout = 5.seconds) {
            val currentTimeMillis = System.currentTimeMillis()
            val digisosSak3: DigisosSak =
                mockk {
                    every { fiksDigisosId } returns "456"
                    every { sistEndret } returns 1000L
                    every { kommunenummer } returns "0301"
                    every { originalSoknadNAV } returns
                        mockk {
                            every { navEksternRefId } returns "123"
                            every { timestampSendt } returns 500L
                        }
                    every { digisosSoker } returns
                        mockk {
                            every { timestampSistOppdatert } returns currentTimeMillis
                        }
                }
            val digisosSak4: DigisosSak =
                mockk {
                    every { fiksDigisosId } returns "123"
                    every { sistEndret } returns currentTimeMillis
                    every { kommunenummer } returns "0301"
                    every { originalSoknadNAV } returns
                        mockk {
                            every { navEksternRefId } returns "456"
                            every { timestampSendt } returns 500L
                        }
                    every { digisosSoker } returns
                        mockk {
                            every { timestampSistOppdatert } returns 1000
                        }
                }

            coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak3, digisosSak4)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(2)
            val firstSak = alleSaker.find { it.fiksDigisosId == "456" }
            assertNotNull(firstSak)
            val nowDate = unixToLocalDateTime(currentTimeMillis)
            assertThat(firstSak.sistOppdatert).isEqualTo(nowDate)
            val secondSak = alleSaker.find { it.fiksDigisosId == "123" }
            assertNotNull(secondSak)
            assertThat(firstSak.sistOppdatert).isEqualTo(nowDate)
        }

    @Test
    internal fun `ikke returner 'tomme' saker`() =
        runTest(timeout = 5.seconds) {
            val tomDigisosSak = DigisosSak("123", "123", "123", "123", 123L, null, null, null, null)
            coEvery { fiksService.getAllSoknader() } returns listOf(tomDigisosSak)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(0)
        }
}
