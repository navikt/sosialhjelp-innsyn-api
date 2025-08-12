package no.nav.sosialhjelp.innsyn.saksoversikt

import io.getunleash.Unleash
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.featuretoggle.FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.KILDE_INNSYN_API
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class SaksOversiktServiceTest {
    private val fiksClient: FiksClient = mockk()
    private val unleashClient: Unleash = mockk()
    private val oppgaveService: OppgaveService = mockk()

    private val saksOversiktService = SaksOversiktService(fiksClient, unleashClient, oppgaveService)

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
            }
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.kommunenummer } returns "0301"
        every { digisosSak2.originalSoknadNAV } returns
            mockk {
                every { navEksternRefId } returns "123"
            }
        every { digisosSak2.digisosSoker } returns mockk()

        every { unleashClient.isEnabled(FAGSYSTEM_MED_INNSYN_I_PAPIRSOKNADER, false) } returns false
    }

    @Test
    internal fun `skal mappe fra DigisosSak til SaksListeResponse`() =
        runTest(timeout = 5.seconds) {
            coEvery { fiksClient.hentAlleDigisosSaker() } returns listOf(digisosSak1, digisosSak2)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(2)
            assertThat(alleSaker[0].fiksDigisosId).isEqualTo("123")
            assertThat(alleSaker[0].soknadTittel).isEqualTo("saker.default_tittel")
            assertThat(alleSaker[0].kilde).isEqualTo(KILDE_INNSYN_API)
            assertThat(alleSaker[0].url).isNull()
            assertThat(alleSaker[1].fiksDigisosId).isEqualTo("456")
            assertThat(alleSaker[1].soknadTittel).isEqualTo("saker.default_tittel")
            assertThat(alleSaker[1].kilde).isEqualTo(KILDE_INNSYN_API)
            assertThat(alleSaker[1].url).isNull()
        }

    @Test
    internal fun `ikke returner 'tomme' saker`() =
        runTest(timeout = 5.seconds) {
            val tomDigisosSak = DigisosSak("123", "123", "123", "123", 123L, null, null, null, null)
            coEvery { fiksClient.hentAlleDigisosSaker() } returns listOf(tomDigisosSak)

            val alleSaker = saksOversiktService.hentAlleSaker()

            assertThat(alleSaker).hasSize(0)
        }
}
