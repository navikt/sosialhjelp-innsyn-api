package no.nav.sosialhjelp.innsyn.saksoversikt

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.klage.KlageRef
import no.nav.sosialhjelp.innsyn.klage.KlageService
import no.nav.sosialhjelp.innsyn.utils.unixToLocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal class SaksOversiktServiceTest {
    private val fiksService: FiksService = mockk()
    private val klageService: KlageService = mockk()

    private val saksOversiktService = SaksOversiktService(fiksService, klageService)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    @BeforeEach
    internal fun setUp() {
        every { digisosSak1.fiksDigisosId } returns UUID.randomUUID().toString()
        every { digisosSak1.sistEndret } returns 2000L
        every { digisosSak1.kommunenummer } returns "0301"
        every { digisosSak1.originalSoknadNAV } returns
            mockk {
                every { navEksternRefId } returns "123"
                every { timestampSendt } returns 1000L
            }
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns UUID.randomUUID().toString()
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
            coEvery { klageService.hentKlager() } returns emptyList()

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(2)
            assertThat(alleSaker[0].fiksDigisosId).isEqualTo(digisosSak2.fiksDigisosId)
            assertThat(alleSaker[0].soknadTittel).isEqualTo("saker.default_tittel")
            assertThat(alleSaker[1].fiksDigisosId).isEqualTo(digisosSak1.fiksDigisosId)
            assertThat(alleSaker[1].soknadTittel).isEqualTo("saker.default_tittel")
        }

    @Test
    internal fun `Skal ta høyeste sistOppdatert`() =
        runTest(timeout = 5.seconds) {
            val currentTimeMillis = System.currentTimeMillis()
            val digisosSak3: DigisosSak =
                mockk {
                    every { fiksDigisosId } returns UUID.randomUUID().toString()
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
                    every { fiksDigisosId } returns UUID.randomUUID().toString()
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
            coEvery { klageService.hentKlager() } returns emptyList()

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(2)
            val firstSak = alleSaker.find { it.fiksDigisosId == digisosSak3.fiksDigisosId }
            assertNotNull(firstSak)
            val nowDate = unixToLocalDateTime(currentTimeMillis)
            assertThat(firstSak.sistOppdatert).isEqualTo(nowDate)
            val secondSak = alleSaker.find { it.fiksDigisosId == digisosSak4.fiksDigisosId }
            assertNotNull(secondSak)
            assertThat(firstSak.sistOppdatert).isEqualTo(nowDate)
        }

    @Test
    internal fun `ikke returner 'tomme' saker`() =
        runTest(timeout = 5.seconds) {
            val tomDigisosSak = DigisosSak(UUID.randomUUID().toString(), "123", "123", "123", 123L, null, null, null, null)
            coEvery { klageService.hentKlager() } returns emptyList()
            coEvery { fiksService.getAllSoknader() } returns listOf(tomDigisosSak)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(0)
        }

    @Test
    internal fun `skal legge på hasKlage`() =
        runTest(timeout = 5.seconds) {
            val klage1 = KlageRef(UUID.randomUUID(), UUID.randomUUID(), UUID.fromString(digisosSak1.fiksDigisosId))
            coEvery { fiksService.getAllSoknader() } returns listOf(digisosSak1, digisosSak2)
            coEvery { klageService.hentKlager() } returns listOf(klage1)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(2)
            assertThat(alleSaker.find { it.fiksDigisosId == digisosSak1.fiksDigisosId }?.hasKlage).isTrue
            assertThat(alleSaker.find { it.fiksDigisosId == digisosSak2.fiksDigisosId }?.hasKlage).isFalse
        }
}
