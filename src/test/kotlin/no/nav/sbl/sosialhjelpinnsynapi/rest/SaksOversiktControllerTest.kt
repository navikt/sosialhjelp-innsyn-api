package no.nav.sbl.sosialhjelpinnsynapi.rest

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.sosialhjelpinnsynapi.client.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.domain.OppgaveResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.Sak
import no.nav.sbl.sosialhjelpinnsynapi.domain.SaksStatus
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus.MOTTATT
import no.nav.sbl.sosialhjelpinnsynapi.domain.SoknadsStatus.UNDER_BEHANDLING
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.service.oppgave.OppgaveService
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.KILDE_INNSYN_API
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class SaksOversiktControllerTest {

    private val fiksClient: FiksClient = mockk()
    private val eventService: EventService = mockk()
    private val oppgaveService: OppgaveService = mockk()

    private val controller = SaksOversiktController(fiksClient, eventService, oppgaveService)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    private val model1: InternalDigisosSoker = mockk()
    private val model2: InternalDigisosSoker = mockk()

    private val sak1: Sak = mockk()
    private val sak2: Sak = mockk()

    private val oppgaveResponseMock: OppgaveResponse = mockk()

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.digisosSoker } returns mockk()

        every { oppgaveResponseMock.oppgaveElementer.size } returns 1

        every { oppgaveService.hentOppgaver("123", any()) } returns listOf(oppgaveResponseMock, oppgaveResponseMock) // 2 oppgaver
        every { oppgaveService.hentOppgaver("456", any()) } returns listOf(oppgaveResponseMock) // 1 oppgave

        every { fiksClient.hentAlleDigisosSaker2(any()) } returns emptyList()
    }

    @Test
    fun `skal mappe fra DigisosSak til SakResponse`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(digisosSak1, digisosSak2)

        every { model1.status } returns MOTTATT
        every { model2.status } returns UNDER_BEHANDLING

        every { model1.oppgaver.isEmpty() } returns false
        every { model2.oppgaver.isEmpty() } returns false

        every { sak1.tittel } returns "Livsopphold"
        every { sak2.tittel } returns "Strøm"

        every { model2.saker } returns mutableListOf(sak1, sak2)

        val response = controller.hentAlleSaker("token")

        val saker = response.body
        assertThat(saker).isNotNull
        assertThat(saker).hasSize(2)

        if (saker != null && saker.size == 2) {
            val first = saker[0]
            assertThat(first.soknadTittel).isEqualTo("Søknad om økonomisk sosialhjelp")
            assertThat(first.kilde).isEqualTo(KILDE_INNSYN_API)

            val second = saker[1]
            assertThat(second.soknadTittel).isEqualTo("Søknad om økonomisk sosialhjelp")
            assertThat(second.kilde).isEqualTo(KILDE_INNSYN_API)
        }
    }

    @Test
    fun `skal mappe fra DigisosSak til SakResponse for detaljer`() {
        every { fiksClient.hentDigisosSak("123", "token", true) } returns digisosSak1
        every { fiksClient.hentDigisosSak("456", "token", true) } returns digisosSak2
        every { eventService.createSaksoversiktModel(any(), digisosSak1) } returns model1
        every { eventService.createSaksoversiktModel(any(), digisosSak2) } returns model2

        every { model1.status } returns MOTTATT
        every { model2.status } returns UNDER_BEHANDLING

        every { model1.oppgaver.isEmpty() } returns false
        every { model2.oppgaver.isEmpty() } returns false

        every { sak1.tittel } returns "Livsopphold"
        every { sak1.saksStatus } returns SaksStatus.UNDER_BEHANDLING
        every { sak2.tittel } returns "Strøm"
        every { sak2.saksStatus } returns SaksStatus.UNDER_BEHANDLING

        every { model1.saker } returns mutableListOf()
        every { model2.saker } returns mutableListOf(sak1, sak2)

        val response1 = controller.hentSaksDetaljer("123", "token")
        val sak1 = response1.body

        assertThat(response1.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(sak1).isNotNull
        assertThat(sak1?.soknadTittel).isEqualTo("")
        assertThat(sak1?.antallNyeOppgaver).isEqualTo(2)

        val response2 = controller.hentSaksDetaljer("456", "token")
        val sak2 = response2.body

        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(sak2).isNotNull
        assertThat(sak2?.soknadTittel).contains("Livsopphold", "Strøm")
        assertThat(sak2?.status).isEqualTo("UNDER BEHANDLING")
        assertThat(sak2?.antallNyeOppgaver).isEqualTo(1)
    }

    @Test
    fun `hvis model ikke har noen oppgaver, skal ikke oppgaveService kalles`() {
        every { fiksClient.hentDigisosSak("123", "token", true) } returns digisosSak1
        every { eventService.createSaksoversiktModel(any(), digisosSak1) } returns model1

        every { model1.status } returns MOTTATT
        every { model1.oppgaver.isEmpty() } returns true
        every { model1.saker } returns mutableListOf()

        val response = controller.hentSaksDetaljer(digisosSak1.fiksDigisosId, "token")
        val sak = response.body

        assertThat(sak).isNotNull
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        verify { oppgaveService wasNot Called }

        assertThat(sak?.antallNyeOppgaver).isEqualTo(null)
    }
}