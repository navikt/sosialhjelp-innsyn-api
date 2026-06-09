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
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

internal class InnsynServiceTest {
    private val fiksService: FiksService = mockk()
    private val kommuneService: KommuneService = mockk()
    private val service = InnsynService(fiksService, kommuneService)
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
                fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any())
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
            coEvery { fiksService.getDocument(any(), any(), JsonSoknad::class.java, any()) } returns mockJsonSoknad

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
            coVerify(exactly = 0) { fiksService.getDocument(any(), any(), JsonDigisosSoker::class.java, any()) }
        }

    @Test
    suspend fun `Bulk-innehenting skal deles opp i chunks`() {
        val antallSaker = 504
        val digisosSaker = createDigisosSaker(antallSaker)

        digisosSaker
            .chunked(InnsynService.CHUNK_SIZE)
            .forEach { saker -> coEvery { fiksService.getAllInnsynsfiler(saker) } returns saker.mapToJsonDigisosSoker() }

        service
            .hentJsonDigisosSokerBulk(digisosSaker)
            .keys
            .also { keys ->
                assertThat(keys.size).isEqualTo(antallSaker)
                digisosSaker
                    .map { sak -> sak.fiksDigisosId }
                    .containsAll(keys)
            }

        val add = if (digisosSaker.size % InnsynService.CHUNK_SIZE == 0) 0 else 1
        coVerify(exactly = ((digisosSaker.size / InnsynService.CHUNK_SIZE) + add)) { fiksService.getAllInnsynsfiler(any()) }
    }

    private fun createDigisosSaker(n: Int): List<DigisosSak> =
        buildList {
            for (i in 1..n) {
                sosialhjelpJsonMapper
                    .readValue(ok_digisossak_response, DigisosSak::class.java)
                    .copy(fiksDigisosId = UUID.randomUUID().toString(), sokerFnr = i.toString())
                    .also { add(it) }
            }
        }.toList()

    private fun List<DigisosSak>.mapToJsonDigisosSoker(): Map<String, JsonDigisosSoker> =
        associate {
            val jsonDigisosSoker: JsonDigisosSoker = mockk()
            it.fiksDigisosId to jsonDigisosSoker
        }
}
