package no.nav.sosialhjelp.innsyn.saksoversikt

import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksException
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.DokumentasjonkravElement
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.DokumentasjonkravResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveElement
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveResponse
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.OppgaveService
import no.nav.sosialhjelp.innsyn.digisossak.oppgaver.VilkarResponse
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus.MOTTATT
import no.nav.sosialhjelp.innsyn.domain.SoknadsStatus.UNDER_BEHANDLING
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.runTestWithToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime

internal class SaksOversiktControllerTest {
    private val saksOversiktService: SaksOversiktService = mockk()
    private val fiksService: FiksService = mockk()
    private val eventService: EventService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val tilgangskontroll: TilgangskontrollService = mockk()

    private val controller =
        SaksOversiktController(saksOversiktService, fiksService, eventService, oppgaveService, tilgangskontroll)

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

        coEvery { tilgangskontroll.sjekkTilgang() } just Runs

        every { digisosSak1.fiksDigisosId } returns "123"
        every { digisosSak1.sistEndret } returns 0L
        every { digisosSak1.digisosSoker } returns null

        every { digisosSak2.fiksDigisosId } returns "456"
        every { digisosSak2.sistEndret } returns 1000L
        every { digisosSak2.digisosSoker } returns mockk()

        every { oppgaveResponseMock.oppgaveElementer } returns listOf(oppgaveElement1)
        every { oppgaveResponseMock.innsendelsesfrist } returns LocalDate.now()
        every { dokumentasjonkravResponseMock.dokumentasjonkravElementer } returns listOf(dokumentasjonkravElement1)
        every { dokumentasjonkravResponseMock.frist } returns LocalDate.now()

        coEvery { oppgaveService.hentOppgaver("123") } returns listOf(oppgaveResponseMock, oppgaveResponseMock) // 2 oppgaver
        coEvery { oppgaveService.hentOppgaver("456") } returns listOf(oppgaveResponseMock) // 1 oppgave
        coEvery { oppgaveService.getVilkar("123") } returns listOf(vilkarResponseMock, vilkarResponseMock) // 2 oppgaver
        coEvery { oppgaveService.getVilkar("456") } returns listOf(vilkarResponseMock) // 1 oppgave
        coEvery {
            oppgaveService.getDokumentasjonkrav("123")
        } returns listOf(dokumentasjonkravResponseMock, dokumentasjonkravResponseMock) // 2 oppgaver
        coEvery { oppgaveService.getDokumentasjonkrav("456") } returns listOf(dokumentasjonkravResponseMock) // 1 oppgave
    }

    @AfterEach
    internal fun tearDown() {
    }

    @Test
    internal fun `skal returnere 503 ved FiksException`() =
        runTestWithToken {
            coEvery { saksOversiktService.hentAlleSaker() } throws FiksException("message", null)

            val response = controller.hentAlleSaker()

            assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        }

    @Test
    fun `skal mappe fra DigisosSak til SakResponse for detaljer`() =
        runTestWithToken {
            coEvery { fiksService.getSoknad("123") } returns digisosSak1
            coEvery { fiksService.getSoknad("456") } returns digisosSak2
            coEvery { eventService.createSaksoversiktModel(digisosSak1) } returns model1
            coEvery { eventService.createSaksoversiktModel(digisosSak2) } returns model2

            every { model1.status } returns MOTTATT
            every { model2.status } returns UNDER_BEHANDLING

            every { model1.forelopigSvar } returns ForelopigSvar(false, null)
            every { model2.forelopigSvar } returns ForelopigSvar(false, null)

            every { model1.oppgaver } returns mutableListOf(mockk())
            every { model2.oppgaver } returns mutableListOf(mockk())

            every { model1.vilkar } returns mutableListOf(mockk())
            every { model2.vilkar } returns mutableListOf(mockk())

            every { model1.dokumentasjonkrav } returns mutableListOf(mockk())
            every { model2.dokumentasjonkrav } returns mutableListOf(mockk())

            every { sak1.tittel } returns "Livsopphold"
            every { sak1.saksStatus } returns SaksStatus.UNDER_BEHANDLING
            every { sak1.vedtak } returns mutableListOf()
            every { sak2.tittel } returns "Strøm"
            every { sak2.saksStatus } returns SaksStatus.UNDER_BEHANDLING
            every { sak2.vedtak } returns mutableListOf()

            every { model1.saker } returns mutableListOf()
            every { model2.saker } returns mutableListOf(sak1, sak2)

            every { model1.utbetalinger } returns mutableListOf()
            every { model2.utbetalinger } returns mutableListOf()

            val date = LocalDateTime.now()
            every {
                model1.historikk
            } returns mutableListOf(Hendelse(hendelseType = HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN, date))

            val date2 = LocalDateTime.now()
            every {
                model2.historikk
            } returns mutableListOf(Hendelse(hendelseType = HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN, date2))

            val sak1 = controller.getSaksDetaljer("123")

            assertThat(sak1).isNotNull
            assertThat(sak1.soknadTittel).isEqualTo("")
            assertThat(sak1.antallNyeOppgaver).isEqualTo(6)
            assertThat(sak1.mottattTidspunkt).isEqualTo(date)

            val sak2 = controller.getSaksDetaljer("456")

            assertThat(sak2).isNotNull
            assertThat(sak2.soknadTittel).contains("Livsopphold", "Strøm")
            assertThat(sak2.status).isEqualTo(UNDER_BEHANDLING)
            assertThat(sak2.antallNyeOppgaver).isEqualTo(3)
            assertThat(sak2.mottattTidspunkt).isEqualTo(date2)
        }

    @Test
    fun `hvis model ikke har noen oppgaver, skal ikke oppgaveService kalles`() =
        runTestWithToken {
            coEvery { fiksService.getSoknad("123") } returns digisosSak1
            coEvery { eventService.createSaksoversiktModel(digisosSak1) } returns model1

            every { model1.status } returns MOTTATT
            every { model1.forelopigSvar } returns ForelopigSvar(false, null)
            every { model1.oppgaver } returns mutableListOf()
            every { model1.vilkar } returns mutableListOf()
            every { model1.dokumentasjonkrav } returns mutableListOf()
            every { model1.saker } returns mutableListOf()
            every { model1.utbetalinger } returns mutableListOf()
            every { model1.historikk } returns mutableListOf()

            val sak = controller.getSaksDetaljer(digisosSak1.fiksDigisosId)

            assertThat(sak).isNotNull

            verify { oppgaveService wasNot Called }

            assertThat(sak.antallNyeOppgaver).isEqualTo(0)
        }
}
