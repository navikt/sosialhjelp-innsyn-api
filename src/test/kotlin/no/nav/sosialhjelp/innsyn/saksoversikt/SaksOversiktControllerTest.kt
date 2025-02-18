package no.nav.sosialhjelp.innsyn.saksoversikt

import io.micrometer.core.instrument.MeterRegistry
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
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
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.time.Duration.Companion.seconds

internal class SaksOversiktControllerTest {
    private val saksOversiktService: SaksOversiktService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val eventService: EventService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val meterRegistry: MeterRegistry = mockk(relaxed = true)

    private val controller =
        SaksOversiktController(saksOversiktService, fiksClient, eventService, oppgaveService, tilgangskontroll, meterRegistry)

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

        coEvery { tilgangskontroll.sjekkTilgang("token") } just Runs

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.digisosSoker } returns mockk()

        every { oppgaveResponseMock.oppgaveElementer } returns listOf(oppgaveElement1)
        every { dokumentasjonkravResponseMock.dokumentasjonkravElementer } returns listOf(dokumentasjonkravElement1)

        coEvery { oppgaveService.hentOppgaver("123", any()) } returns listOf(oppgaveResponseMock, oppgaveResponseMock) // 2 oppgaver
        coEvery { oppgaveService.hentOppgaver("456", any()) } returns listOf(oppgaveResponseMock) // 1 oppgave
        coEvery { oppgaveService.getVilkar("123", any()) } returns listOf(vilkarResponseMock, vilkarResponseMock) // 2 oppgaver
        coEvery { oppgaveService.getVilkar("456", any()) } returns listOf(vilkarResponseMock) // 1 oppgave
        coEvery {
            oppgaveService.getDokumentasjonkrav("123", any())
        } returns listOf(dokumentasjonkravResponseMock, dokumentasjonkravResponseMock) // 2 oppgaver
        coEvery { oppgaveService.getDokumentasjonkrav("456", any()) } returns listOf(dokumentasjonkravResponseMock) // 1 oppgave
    }

    @AfterEach
    internal fun tearDown() {
    }

    @Test
    internal fun `skal returnere 503 ved FiksException`() =
        runTest(timeout = 5.seconds) {
            coEvery { saksOversiktService.hentAlleSaker(any()) } throws FiksException("message", null)

            val response = controller.hentAlleSaker("token")

            assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        }

    @Test
    fun `skal mappe fra DigisosSak til SakResponse for detaljer`() =
        runTest(timeout = 5.seconds) {
            coEvery { fiksClient.hentDigisosSak("123", "token") } returns digisosSak1
            coEvery { fiksClient.hentDigisosSak("456", "token") } returns digisosSak2
            coEvery { eventService.createSaksoversiktModel(digisosSak1, any()) } returns model1
            coEvery { eventService.createSaksoversiktModel(digisosSak2, any()) } returns model2

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

            every { model1.utbetalinger } returns mutableListOf()
            every { model2.utbetalinger } returns mutableListOf()

            val sak1 = controller.getSaksDetaljer("123", "token")

            assertThat(sak1).isNotNull
            assertThat(sak1.soknadTittel).isEqualTo("")
            assertThat(sak1.antallNyeOppgaver).isEqualTo(6)

            val sak2 = controller.getSaksDetaljer("456", "token")

            assertThat(sak2).isNotNull
            assertThat(sak2.soknadTittel).contains("Livsopphold", "Strøm")
            assertThat(sak2.status).isEqualTo("UNDER_BEHANDLING")
            assertThat(sak2.antallNyeOppgaver).isEqualTo(3)
        }

    @Test
    fun `hvis model ikke har noen oppgaver, skal ikke oppgaveService kalles`() =
        runTest(timeout = 5.seconds) {
            coEvery { fiksClient.hentDigisosSak("123", "token") } returns digisosSak1
            coEvery { eventService.createSaksoversiktModel(digisosSak1, any()) } returns model1

            every { model1.status } returns MOTTATT
            every { model1.oppgaver } returns mutableListOf()
            every { model1.vilkar } returns mutableListOf()
            every { model1.dokumentasjonkrav } returns mutableListOf()
            every { model1.saker } returns mutableListOf()
            every { model1.utbetalinger } returns mutableListOf()

            val sak = controller.getSaksDetaljer(digisosSak1.fiksDigisosId, "token")

            assertThat(sak).isNotNull

            verify { oppgaveService wasNot Called }

            assertThat(sak.antallNyeOppgaver).isEqualTo(0)
        }
}
