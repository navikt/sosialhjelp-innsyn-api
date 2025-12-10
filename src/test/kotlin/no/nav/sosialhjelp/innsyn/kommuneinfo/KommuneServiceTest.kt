package no.nav.sosialhjelp.innsyn.kommuneinfo

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.KommuneInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class KommuneServiceTest {
    private val fiksService: FiksService = mockk()
    private val kommuneInfoClient: KommuneInfoClient = mockk()
    private val service = KommuneService(fiksService, kommuneInfoClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val kommuneNr = "1234"

    @BeforeEach
    internal fun setUp() {
        clearMocks(fiksService, mockDigisosSak)

        coEvery { fiksService.getSoknad(any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some id"
        every { mockDigisosSak.kommunenummer } returns kommuneNr
    }

    @Test
    internal fun `innsyn er deaktivert`() =
        runTest(timeout = 5.seconds) {
            coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
                KommuneInfo(
                    kommunenummer = kommuneNr,
                    kanMottaSoknader = false,
                    kanOppdatereStatus = false,
                    harMidlertidigDeaktivertMottak = false,
                    harMidlertidigDeaktivertOppdateringer = false,
                    kontaktpersoner = null,
                    harNksTilgang = true,
                    behandlingsansvarlig = null,
                )

            val svar = service.erInnsynDeaktivertForKommune("123")

            assertThat(svar).isTrue
        }

    @Test
    internal fun `innsyn er aktivert`() =
        runTest(timeout = 5.seconds) {
            coEvery { kommuneInfoClient.getKommuneInfo(any()) } returns
                KommuneInfo(
                    kommunenummer = kommuneNr,
                    kanMottaSoknader = false,
                    kanOppdatereStatus = true,
                    harMidlertidigDeaktivertMottak = false,
                    harMidlertidigDeaktivertOppdateringer = false,
                    kontaktpersoner = null,
                    harNksTilgang = true,
                    behandlingsansvarlig = null,
                )

            val svar = service.erInnsynDeaktivertForKommune("123")

            assertThat(svar).isFalse
        }
}
