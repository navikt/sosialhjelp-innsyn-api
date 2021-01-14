package no.nav.sbl.sosialhjelpinnsynapi.service.soknadsstatus

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.service.kommune.KommuneService
import no.nav.sbl.sosialhjelpinnsynapi.utils.soknadsalderIMinutter
import no.nav.sosialhjelp.api.fiks.DigisosSak
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

    @BeforeEach
    fun init() {
        clearMocks(eventService, mockInternalDigisosSoker, fiksClient)
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { kommuneService.erInnsynDeaktivertForKommune(any(), any()) } returns false
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns "dokumentlagerId"
    }

    @Test
    fun `Skal returnere mest nylige SoknadsStatus`() {
        every { eventService.createModel(any(), any()) } returns mockInternalDigisosSoker
        every { mockInternalDigisosSoker.status } returns SoknadsStatus.UNDER_BEHANDLING
        every { mockInternalDigisosSoker.tidspunktSendt } returns null
        every { mockInternalDigisosSoker.soknadsmottaker?.navEnhetsnavn } returns "NAV Test"

        val response = service.hentSoknadsStatus("123", token)

        assertThat(response).isNotNull
        assertThat(response.status).isEqualTo(SoknadsStatus.UNDER_BEHANDLING)
    }

    @Test
    fun `Skal regne soknadens alder`() {
        val tidspunktSendt = LocalDateTime.now().minusDays(1).plusHours(2).minusMinutes(3) // (24-2)h * 60 m/h - 3 = 22*60-3 =
        val response = soknadsalderIMinutter(tidspunktSendt)

        assertThat(response).isEqualTo(1323) // (24-2)h * 60 m/h + 3 = 22*60+3
    }
}