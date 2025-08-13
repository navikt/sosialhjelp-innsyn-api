package no.nav.sosialhjelp.innsyn.event

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DigisosSoker
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class InnsynServiceTest {
    private val fiksClient: FiksClient = mockk()
    private val kommuneService: KommuneService = mockk()
    private val service = InnsynService(fiksClient, kommuneService)
    private val originalSoknad: OriginalSoknadNAV = mockk()
    private val digisosSoker: DigisosSoker = mockk()
    private val digisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearAllMocks()

        coEvery { kommuneService.erInnsynDeaktivertForKommune(any()) } returns false
        every { originalSoknad.metadata } returns "metadata"
        every { digisosSoker.metadata } returns "metadata"
        every { digisosSak.originalSoknadNAV } returns originalSoknad
        every { digisosSak.fiksDigisosId } returns "fiksDigisosId"
        every { digisosSak.digisosSoker } returns digisosSoker
        every { digisosSoker.timestampSistOppdatert } returns 123L
    }

    @Test
    fun `Skal hente innsyn data`() =
        runTest(timeout = 5.seconds) {
            val mockJsonDigisosSoker: JsonDigisosSoker = mockk()

            coEvery {
                fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any())
            } returns mockJsonDigisosSoker

            val jsonDigisosSoker: JsonDigisosSoker? = service.hentJsonDigisosSoker(digisosSak)

            assertThat(jsonDigisosSoker).isNotNull
        }

    @Test
    fun `Skal returnere null hvis JsonDigisosSoker er null`() =
        runTest(timeout = 5.seconds) {
            every { digisosSak.digisosSoker } returns null
            val jsonDigisosSoker = service.hentJsonDigisosSoker(digisosSak)

            assertThat(jsonDigisosSoker).isNull()
        }

    @Test
    fun `Skal returnere originalSoknad`() =
        runTest(timeout = 5.seconds) {
            val mockJsonSoknad: JsonSoknad = mockk()
            coEvery { fiksClient.hentDokument(any(), any(), JsonSoknad::class.java, any()) } returns mockJsonSoknad

            val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad(digisosSak)

            assertThat(jsonSoknad).isNotNull
        }

    @Test
    fun `Skal returnere null hvis originalSoknadNAV er null`() =
        runTest(timeout = 5.seconds) {
            every { digisosSak.originalSoknadNAV } returns null
            val jsonSoknad: JsonSoknad? = service.hentOriginalSoknad(digisosSak)

            assertThat(jsonSoknad).isNull()
        }

    @Test
    internal fun `Skal ikke hente innsynsdata hvis kommunen har deaktivert innsyn`() =
        runTest(timeout = 5.seconds) {
            coEvery { kommuneService.erInnsynDeaktivertForKommune(any()) } returns true

            assertThat(service.hentJsonDigisosSoker(digisosSak)).isNull()
            coVerify(exactly = 0) { fiksClient.hentDokument(any(), any(), JsonDigisosSoker::class.java, any()) }
        }
}
