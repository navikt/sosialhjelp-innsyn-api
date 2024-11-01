package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

internal class SoknadsStatusServiceTest {
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val kommuneService: KommuneService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    private val service = SoknadsStatusService(eventService, fiksClient, kommuneService, clientProperties)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockInternalDigisosSoker: InternalDigisosSoker = mockk()

    private val token = "token"
    private val dokumentlagerId = "dokumentlagerId"
    private val navEnhet = "Nav Test"

    @BeforeEach
    fun init() {
        clearAllMocks()
        coEvery { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        // TODO fjerne når feilsøk er ferdig
        coEvery { fiksClient.hentDigisosSakMedFnr(any(), any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns dokumentlagerId
    }

    // TODO fjerne ignore når feilsøk er ferdig.
    @Disabled("Kjøres ikke mens uthenting av fnr fra kontekst gjøres for feilsøking")
    @Test
    fun `Skal returnere nyeste SoknadsStatus - innsyn aktivert`() =
        runTest(timeout = 5.seconds) {
            val now = LocalDateTime.now()
            coEvery { eventService.createModel(any(), any()) } returns mockInternalDigisosSoker
            every { mockInternalDigisosSoker.status } returns SoknadsStatus.UNDER_BEHANDLING
            every { mockInternalDigisosSoker.tidspunktSendt } returns now
            every { mockInternalDigisosSoker.soknadsmottaker?.navEnhetsnavn } returns navEnhet
            coEvery { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false

            val response = service.hentSoknadsStatus("123", token, "fnr")

            assertThat(response).isNotNull
            assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(response.tidspunktSendt).isEqualTo(now)
            assertThat(response.navKontor).isNull()
            assertThat(response.soknadUrl).isNull()
        }

    // TODO fjerne ignore etter feilsøk er ferdig.
    @Disabled("Kjøres ikke mens uthenting av fnr fra kontekst gjøres for feilsøking")
    @Test
    fun `Skal returnere nyeste SoknadsStatus - innsyn deaktivert`() =
        runTest(timeout = 5.seconds) {
            val now = LocalDateTime.now()
            coEvery { eventService.createModel(any(), any()) } returns mockInternalDigisosSoker
            every { mockInternalDigisosSoker.status } returns SoknadsStatus.UNDER_BEHANDLING
            every { mockInternalDigisosSoker.tidspunktSendt } returns now
            every { mockInternalDigisosSoker.soknadsmottaker?.navEnhetsnavn } returns navEnhet
            coEvery { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns true

            val response = service.hentSoknadsStatus("123", token, "fnr")

            assertThat(response).isNotNull
            assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
            assertThat(response.tidspunktSendt).isEqualTo(now)
            assertThat(response.navKontor).isEqualTo(navEnhet)
            assertThat(response.soknadUrl?.link).contains(dokumentlagerId)
        }

    @Test
    fun `Skal regne soknadens alder`() {
        val tidspunktSendt =
            LocalDateTime.now().minusDays(1).plusHours(2).minusMinutes(3) // (24-2)h * 60 m/h - 3 = 22*60-3 =
        val response = soknadsalderIMinutter(tidspunktSendt)

        assertThat(response).isEqualTo(1323) // (24-2)h * 60 m/h + 3 = 22*60+3
    }
}
