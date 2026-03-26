package no.nav.sosialhjelp.innsyn.digisossak.brev

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
import no.nav.sosialhjelp.innsyn.domain.ForelopigSvar
import no.nav.sosialhjelp.innsyn.domain.Forvaltningsbrev
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgave
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Vedtak
import no.nav.sosialhjelp.innsyn.event.EventService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class BrevServiceTest {
    private val fiksService: FiksService = mockk()
    private val eventService: EventService = mockk()

    private val service = BrevService(fiksService, eventService)

    private val mockDigisosSak: DigisosSak = mockk()

    @BeforeEach
    fun init() {
        clearMocks(fiksService, eventService)
        coEvery { fiksService.getSoknad(any()) } returns mockDigisosSak
    }

    @Test
    suspend fun `Skal returnere tom liste når modellen er tom`() {
        coEvery { eventService.createModel(any()) } returns InternalDigisosSoker()

        val result = service.getBrev("123")

        assertThat(result).isEmpty()
    }

    @Test
    suspend fun `Skal returnere forelopigSvar-brev når harMottattForelopigSvar er true og link er satt`() {
        val link = "https://nav.no/brev/forelopig"
        val timestamp = LocalDateTime.now()
        val model =
            InternalDigisosSoker(
                forelopigSvar = ForelopigSvar(harMottattForelopigSvar = true, link = link, timestamp = timestamp),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(Brev.BrevType.FORELOPIG_SVAR)
        assertThat(result[0].url).isEqualTo(link)
        assertThat(result[0].timestamp).isEqualTo(timestamp)
    }

    @Test
    suspend fun `Skal ikke returnere forelopigSvar-brev når harMottattForelopigSvar er false`() {
        val model =
            InternalDigisosSoker(
                forelopigSvar = ForelopigSvar(harMottattForelopigSvar = false, link = "https://nav.no/brev/forelopig"),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).isEmpty()
    }

    @Test
    suspend fun `Skal ikke returnere forelopigSvar-brev når link er null`() {
        val model =
            InternalDigisosSoker(
                forelopigSvar = ForelopigSvar(harMottattForelopigSvar = true, link = null),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).isEmpty()
    }

    @Test
    suspend fun `Skal returnere vedtaksbrev fra saker`() {
        val vedtaksFilUrl = "https://nav.no/vedtak/123"
        val dato = LocalDate.of(2024, 1, 15)
        val model =
            InternalDigisosSoker(
                saker =
                    mutableListOf(
                        Sak(
                            referanse = "ref1",
                            saksStatus = SaksStatus.FERDIGBEHANDLET,
                            tittel = "Livsopphold",
                            vedtak = mutableListOf(Vedtak(id = "v1", utfall = null, vedtaksFilUrl = vedtaksFilUrl, dato = dato)),
                            utbetalinger = mutableListOf(),
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(Brev.BrevType.VEDTAK)
        assertThat(result[0].url).isEqualTo(vedtaksFilUrl)
        assertThat(result[0].timestamp).isEqualTo(dato.atStartOfDay())
    }

    @Test
    suspend fun `Skal returnere vedtaksbrev med null timestamp når dato er null`() {
        val model =
            InternalDigisosSoker(
                saker =
                    mutableListOf(
                        Sak(
                            referanse = "ref1",
                            saksStatus = SaksStatus.FERDIGBEHANDLET,
                            tittel = "Livsopphold",
                            vedtak =
                                mutableListOf(
                                    Vedtak(id = "v1", utfall = null, vedtaksFilUrl = "https://nav.no/vedtak", dato = null),
                                ),
                            utbetalinger = mutableListOf(),
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(Brev.BrevType.VEDTAK)
        assertThat(result[0].timestamp).isNull()
    }

    @Test
    suspend fun `Skal returnere vedtaksbrev fra alle vedtak på tvers av saker`() {
        val model =
            InternalDigisosSoker(
                saker =
                    mutableListOf(
                        Sak(
                            referanse = "ref1",
                            saksStatus = SaksStatus.FERDIGBEHANDLET,
                            tittel = "Livsopphold",
                            vedtak =
                                mutableListOf(
                                    Vedtak(id = "v1", utfall = null, vedtaksFilUrl = "https://nav.no/vedtak/1", dato = null),
                                    Vedtak(id = "v2", utfall = null, vedtaksFilUrl = "https://nav.no/vedtak/2", dato = null),
                                ),
                            utbetalinger = mutableListOf(),
                        ),
                        Sak(
                            referanse = "ref2",
                            saksStatus = SaksStatus.FERDIGBEHANDLET,
                            tittel = "Tannlege",
                            vedtak =
                                mutableListOf(
                                    Vedtak(id = "v3", utfall = null, vedtaksFilUrl = "https://nav.no/vedtak/3", dato = null),
                                ),
                            utbetalinger = mutableListOf(),
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(3)
        assertThat(result).allMatch { it.type == Brev.BrevType.VEDTAK }
    }

    @Test
    suspend fun `Skal returnere dokumentasjonEtterspurt-brev fra oppgaver med forvaltningsbrev`() {
        val url = "https://nav.no/brev/dok"
        val timestamp = LocalDateTime.now()
        val model =
            InternalDigisosSoker(
                oppgaver =
                    mutableListOf(
                        Oppgave(
                            oppgaveId = "o1",
                            tittel = "type",
                            tilleggsinfo = null,
                            hendelsetype = null,
                            hendelsereferanse = null,
                            innsendelsesfrist = null,
                            tidspunktForKrav = LocalDateTime.now(),
                            erFraInnsyn = true,
                            forvaltningsbrev = Forvaltningsbrev(url = url, timestamp = timestamp),
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(Brev.BrevType.DOKUMENTASJON_ETTERSPURT)
        assertThat(result[0].url).isEqualTo(url)
        assertThat(result[0].timestamp).isEqualTo(timestamp)
    }

    @Test
    suspend fun `Skal ignorere oppgaver uten forvaltningsbrev`() {
        val model =
            InternalDigisosSoker(
                oppgaver =
                    mutableListOf(
                        Oppgave(
                            oppgaveId = "o1",
                            tittel = "type",
                            tilleggsinfo = null,
                            hendelsetype = null,
                            hendelsereferanse = null,
                            innsendelsesfrist = null,
                            tidspunktForKrav = LocalDateTime.now(),
                            erFraInnsyn = true,
                            forvaltningsbrev = null,
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).isEmpty()
    }

    @Test
    suspend fun `Skal deduplisere dokumentasjonEtterspurt-brev med samme url`() {
        val url = "https://nav.no/brev/dok"
        val timestamp = LocalDateTime.now()
        val model =
            InternalDigisosSoker(
                oppgaver =
                    mutableListOf(
                        Oppgave(
                            oppgaveId = "o1",
                            tittel = "type1",
                            tilleggsinfo = null,
                            hendelsetype = null,
                            hendelsereferanse = null,
                            innsendelsesfrist = null,
                            tidspunktForKrav = LocalDateTime.now(),
                            erFraInnsyn = true,
                            forvaltningsbrev = Forvaltningsbrev(url = url, timestamp = timestamp),
                        ),
                        Oppgave(
                            oppgaveId = "o2",
                            tittel = "type2",
                            tilleggsinfo = null,
                            hendelsetype = null,
                            hendelsereferanse = null,
                            innsendelsesfrist = null,
                            tidspunktForKrav = LocalDateTime.now(),
                            erFraInnsyn = true,
                            forvaltningsbrev = Forvaltningsbrev(url = url, timestamp = timestamp),
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(1)
    }

    @Test
    suspend fun `Skal sortere brev etter timestamp synkende`() {
        val oldest = LocalDate.of(2023, 1, 1)
        val middle = LocalDateTime.of(2024, 6, 15, 12, 0)
        val newest = LocalDateTime.of(2025, 3, 1, 9, 0)
        val model =
            InternalDigisosSoker(
                forelopigSvar = ForelopigSvar(harMottattForelopigSvar = true, link = "https://nav.no/forelopig", timestamp = middle),
                saker =
                    mutableListOf(
                        Sak(
                            referanse = "ref1",
                            saksStatus = SaksStatus.FERDIGBEHANDLET,
                            tittel = "Livsopphold",
                            vedtak =
                                mutableListOf(
                                    Vedtak(id = "v1", utfall = null, vedtaksFilUrl = "https://nav.no/vedtak", dato = oldest),
                                ),
                            utbetalinger = mutableListOf(),
                        ),
                    ),
                oppgaver =
                    mutableListOf(
                        Oppgave(
                            oppgaveId = "o1",
                            tittel = "type",
                            tilleggsinfo = null,
                            hendelsetype = null,
                            hendelsereferanse = null,
                            innsendelsesfrist = null,
                            tidspunktForKrav = LocalDateTime.now(),
                            erFraInnsyn = true,
                            forvaltningsbrev = Forvaltningsbrev(url = "https://nav.no/dok", timestamp = newest),
                        ),
                    ),
            )
        coEvery { eventService.createModel(any()) } returns model

        val result = service.getBrev("123")

        assertThat(result).hasSize(3)
        assertThat(result[0].timestamp).isEqualTo(newest)
        assertThat(result[1].timestamp).isEqualTo(middle)
        assertThat(result[2].timestamp).isEqualTo(oldest.atStartOfDay())
    }
}
