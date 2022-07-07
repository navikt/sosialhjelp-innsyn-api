package no.nav.sosialhjelp.innsyn.digisossak.soknadsstatus

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.kommuneinfo.KommuneService
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

    private val token = "token"
    private val dokumentlagerId = "dokumentlagerId"
    private val navEnhet = "NAV Test"

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns dokumentlagerId
    }

    @Test
    fun `Skal returnere nyeste SoknadsStatus - innsyn aktivert`() {
        val now = LocalDateTime.now()
        every { eventService.createModel(any(), any()) } returns mockInternalDigisosSoker
        every { mockInternalDigisosSoker.status } returns SoknadsStatus.UNDER_BEHANDLING
        every { mockInternalDigisosSoker.tidspunktSendt } returns now
        every { mockInternalDigisosSoker.soknadsmottaker?.navEnhetsnavn } returns navEnhet
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false

        val response = service.hentSoknadsStatus("123", token)

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(response.tidspunktSendt).isEqualTo(now)
        assertThat(response.navKontor).isNull()
        assertThat(response.soknadUrl).isNull()
    }

    @Test
    fun `Skal returnere nyeste SoknadsStatus - innsyn deaktivert`() {
        val now = LocalDateTime.now()
        every { eventService.createModel(any(), any()) } returns mockInternalDigisosSoker
        every { mockInternalDigisosSoker.status } returns SoknadsStatus.UNDER_BEHANDLING
        every { mockInternalDigisosSoker.tidspunktSendt } returns now
        every { mockInternalDigisosSoker.soknadsmottaker?.navEnhetsnavn } returns navEnhet
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns true

        val response = service.hentSoknadsStatus("123", token)

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
        assertThat(response.tidspunktSendt).isEqualTo(now)
        assertThat(response.navKontor).isEqualTo(navEnhet)
        assertThat(response.soknadUrl?.link).contains(dokumentlagerId)
    }

    @Test
    fun `Skal regne soknadens alder`() {
        val tidspunktSendt = LocalDateTime.now().minusDays(1).plusHours(2).minusMinutes(3) // (24-2)h * 60 m/h - 3 = 22*60-3 =
        val response = soknadsalderIMinutter(tidspunktSendt)

        assertThat(response).isEqualTo(1323) // (24-2)h * 60 m/h + 3 = 22*60+3
    }
}
