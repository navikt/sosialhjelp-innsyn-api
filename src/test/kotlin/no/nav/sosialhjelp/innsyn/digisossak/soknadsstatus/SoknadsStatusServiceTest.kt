package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
import no.nav.sosialhjelp.innsyn.utils.runTestWithToken
import no.nav.sosialhjelp.innsyn.utils.soknadsalderIMinutter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SoknadsStatusServiceTest {
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val kommuneService: KommuneService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    private val service = SoknadsStatusService(eventService, fiksClient, kommuneService, clientProperties)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockInternalDigisosSoker: InternalDigisosSoker = mockk()

    private val token = Token("token")
    private val dokumentlagerId = "dokumentlagerId"
    private val navEnhet = "Nav Test"

    @BeforeEach
    fun init() {
        clearAllMocks()
        coEvery { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV } returns
            mockk {
                every { soknadDokument.dokumentlagerDokumentId } returns dokumentlagerId
                every { navEksternRefId } returns "23S"
            }
        every { mockDigisosSak.kommunenummer } returns "123"
    }

    @Test
    fun `Skal returnere nyeste SoknadsStatus - innsyn aktivert`() =
        runTestWithToken {
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

    @Test
    fun `Skal returnere nyeste SoknadsStatus - innsyn deaktivert`() =
        runTestWithToken {
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
