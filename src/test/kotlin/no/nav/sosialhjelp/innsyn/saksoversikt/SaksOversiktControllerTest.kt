package no.nav.sosialhjelp.innsyn.saksoversikt

import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.DokumentasjonkravElement
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.DokumentasjonkravResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveElement
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.VilkarResponse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus.MOTTATT
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus.UNDER_BEHANDLING
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.KILDE_INNSYN_API
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class SaksOversiktControllerTest {

    private val fiksClient: FiksClient = mockk()
    private val eventService: EventService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val tilgangskontroll: Tilgangskontroll = mockk()

    private val controller = SaksOversiktController(fiksClient, eventService, oppgaveService, tilgangskontroll)

    private val digisosSak1: DigisosSak = mockk()
    private val digisosSak2: DigisosSak = mockk()

    private val model1: InternalDigisosSoker = mockk()
    private val model2: InternalDigisosSoker = mockk()

    private val sak1: Sak = mockk()
    private val sak2: Sak = mockk()

    private val oppgaveResponseMock: OppgaveResponse = mockk()
    private val vilkarResponseMock: VilkarResponse = mockk()
    private val dokumentasjonkravResponseMock: DokumentasjonkravResponse = mockk()

    private val oppgaveElement1: OppgaveElement = mockk()
    private val dokumentasjonkravElement1: DokumentasjonkravElement = mockk()

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()

        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())

        every { tilgangskontroll.sjekkTilgang("token") } just Runs

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.digisosSoker } returns mockk()

        every { oppgaveResponseMock.oppgaveElementer } returns listOf(oppgaveElement1)
        every { dokumentasjonkravResponseMock.dokumentasjonkravElementer } returns listOf(dokumentasjonkravElement1)

        every { oppgaveService.hentOppgaver("123", any()) } returns listOf(oppgaveResponseMock, oppgaveResponseMock) // 2 oppgaver
        every { oppgaveService.hentOppgaver("456", any()) } returns listOf(oppgaveResponseMock) // 1 oppgave
        every { oppgaveService.getVilkar("123", any()) } returns listOf(vilkarResponseMock, vilkarResponseMock) // 2 oppgaver
        every { oppgaveService.getVilkar("456", any()) } returns listOf(vilkarResponseMock) // 1 oppgave
        every { oppgaveService.getDokumentasjonkrav("123", any()) } returns listOf(dokumentasjonkravResponseMock, dokumentasjonkravResponseMock) // 2 oppgaver
        every { oppgaveService.getDokumentasjonkrav("456", any()) } returns listOf(dokumentasjonkravResponseMock) // 1 oppgave
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
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
        every { eventService.createSaksoversiktModel(digisosSak1, any()) } returns model1
        every { eventService.createSaksoversiktModel(digisosSak2, any()) } returns model2

        every { model1.status } returns MOTTATT
        every { model2.status } returns UNDER_BEHANDLING

        every { model1.oppgaver } returns mutableListOf(mockk())
        every { model2.oppgaver } returns mutableListOf(mockk())

        every { model1.vilkar } returns mutableListOf(mockk())
        every { model2.vilkar } returns mutableListOf(mockk())

        every { model1.dokumentasjonkrav } returns mutableListOf(mockk())
        every { model2.dokumentasjonkrav } returns mutableListOf(mockk())

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
        assertThat(sak1?.antallNyeOppgaver).isEqualTo(6)

        val response2 = controller.hentSaksDetaljer("456", "token")
        val sak2 = response2.body

        assertThat(response2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(sak2).isNotNull
        assertThat(sak2?.soknadTittel).contains("Livsopphold", "Strøm")
        assertThat(sak2?.status).isEqualTo("UNDER BEHANDLING")
        assertThat(sak2?.antallNyeOppgaver).isEqualTo(3)
    }

    @Test
    fun `hvis model ikke har noen oppgaver, skal ikke oppgaveService kalles`() {
        every { fiksClient.hentDigisosSak("123", "token", true) } returns digisosSak1
        every { eventService.createSaksoversiktModel(digisosSak1, any()) } returns model1

        every { model1.status } returns MOTTATT
        every { model1.oppgaver } returns mutableListOf()
        every { model1.vilkar } returns mutableListOf()
        every { model1.dokumentasjonkrav } returns mutableListOf()
        every { model1.saker } returns mutableListOf()

        val response = controller.hentSaksDetaljer(digisosSak1.fiksDigisosId, "token")
        val sak = response.body

        assertThat(sak).isNotNull
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        verify { oppgaveService wasNot Called }

        assertThat(sak?.antallNyeOppgaver).isEqualTo(0)
    }
}
