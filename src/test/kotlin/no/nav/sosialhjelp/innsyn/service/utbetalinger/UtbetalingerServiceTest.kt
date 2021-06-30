package no.nav.sosialhjelp.innsyn.service.utbetalinger

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Dokumentasjonkrav
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingerResponse
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.domain.Vilkar
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.event.apply
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

internal class UtbetalingerServiceTest {
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()

    private val service = UtbetalingerService(eventService, fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()

    private val token = "token"

    private val digisosId = "some id"

    private val tittel = "tittel"
    private val referanse = "referanse"

    private val dokumentasjonkravId = "068e5c6516019eec95f19dd4fd78045aa25b634849538440ba49f7050cdbe4ce"

    @BeforeEach
    fun init() {
        clearAllMocks()

        coEvery { mockDigisosSak.fiksDigisosId } returns digisosId
        coEvery { mockDigisosSak.kommunenummer } returns "kommunenr"
        coEvery { mockDigisosSak.sistEndret } returns DateTime.now().millis
    }

    @Test
    fun `Skal returnere at utbetalinger ikke eksisterer om soker ikke har noen digisosSaker`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns emptyList()

        val response = service.utbetalingExists(token, 6)

        assertThat(response).isFalse()
    }

    @Test
    fun `Skal returnere at utbetalinger ikke eksisterer om soker ikke har utbetalinger på noen digisosSaker`() {
        val model = InternalDigisosSoker()
        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response = service.utbetalingExists(token, 6)

        assertThat(response).isFalse()
    }

    @Test
    fun `Skal returnere at utbetalinger ikke eksisterer om det digisosSak inneholder utebtalinger som tom liste`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf()

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response = service.utbetalingExists(token, 6)

        assertThat(response).isFalse()
    }

    @Test
    fun `Skal returnere at utbetalinger ikke eksisterer om det finnes 1 gammel utbetaling`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling(
                referanse = "Sak1",
                status = UtbetalingsStatus.UTBETALT,
                belop = BigDecimal.TEN,
                beskrivelse = "Nødhjelp",
                forfallsDato = null,
                utbetalingsDato = LocalDate.now().minusMonths(13),
                fom = LocalDate.now().minusMonths(13).withDayOfMonth(1),
                tom = LocalDate.now().minusMonths(13).withDayOfMonth(28),
                mottaker = "utleier",
                kontonummer = "kontonr",
                utbetalingsmetode = "utbetalingsmetode",
                annenMottaker = false,
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf(),
                datoHendelse = LocalDateTime.now()
            )
        )

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response = service.utbetalingExists(token, 12)

        assertThat(response).isFalse()
    }

    @Test
    fun `Skal returnere at utbetalinger eksisterer om det finnes 1 utbetaling`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling(
                referanse = "Sak1",
                status = UtbetalingsStatus.UTBETALT,
                belop = BigDecimal.TEN,
                beskrivelse = "Nødhjelp",
                forfallsDato = null,
                utbetalingsDato = LocalDate.now().minusMonths(12),
                fom = LocalDate.now().minusMonths(12).withDayOfMonth(1),
                tom = LocalDate.now().minusMonths(12).withDayOfMonth(28),
                mottaker = "utleier",
                kontonummer = "kontonr",
                utbetalingsmetode = "utbetalingsmetode",
                annenMottaker = false,
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf(),
                datoHendelse = LocalDateTime.now()
            )
        )

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response = service.utbetalingExists(token, 12)

        assertThat(response).isTrue()
    }

    @Test
    fun `Skal returnere emptyList hvis soker ikke har noen digisosSaker`() {
        every { fiksClient.hentAlleDigisosSaker(any()) } returns emptyList()

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isEmpty()
    }

    @Test
    fun `Skal returnere response med 1 utbetaling`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling(
                referanse = "Sak1",
                status = UtbetalingsStatus.UTBETALT,
                belop = BigDecimal.TEN,
                beskrivelse = "Nødhjelp",
                forfallsDato = null,
                utbetalingsDato = LocalDate.of(2019, 8, 10),
                fom = LocalDate.of(2019, 8, 1),
                tom = LocalDate.of(2019, 8, 31),
                mottaker = "utleier",
                kontonummer = "kontonr",
                utbetalingsmetode = "utbetalingsmetode",
                annenMottaker = false,
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf(),
                datoHendelse = LocalDateTime.now()
            )
        )

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotEmpty
        assertThat(response).hasSize(1)
        assertThat(response[0].ar).isEqualTo(2019)
        assertThat(response[0].maned).isEqualToIgnoringCase("august")
        assertThat(response[0].foersteIManeden).isEqualTo(LocalDate.of(2019, 8, 1))
        assertThat(response[0].utbetalinger).hasSize(1)
        assertThat(response[0].utbetalinger[0].tittel).isEqualTo("Nødhjelp")
        assertThat(response[0].utbetalinger[0].belop).isEqualTo(10.0)
        assertThat(response[0].utbetalinger[0].fiksDigisosId).isEqualTo(digisosId)
        assertThat(response[0].utbetalinger[0].utbetalingsdato).isEqualTo("2019-08-10")
        assertThat(response[0].utbetalinger[0].fom).isEqualTo("2019-08-01")
        assertThat(response[0].utbetalinger[0].tom).isEqualTo("2019-08-31")
        assertThat(response[0].utbetalinger[0].mottaker).isEqualTo("utleier")
        assertThat(response[0].utbetalinger[0].kontonummer).isEqualTo("kontonr")
        assertThat(response[0].utbetalinger[0].utbetalingsmetode).isEqualTo("utbetalingsmetode")
    }

    @Test
    fun `Skal returnere response med 2 utbetalinger for 1 maned`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", null, LocalDate.of(2019, 8, 10), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now()),
            Utbetaling("Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Tannlege", null, LocalDate.of(2019, 8, 12), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        )

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].ar).isEqualTo(2019)
        assertThat(response[0].maned).isEqualToIgnoringCase("august")
        assertThat(response[0].foersteIManeden).isEqualTo(LocalDate.of(2019, 8, 1))
        assertThat(response[0].utbetalinger).hasSize(2)
        assertThat(response[0].utbetalinger[0].tittel).isEqualTo("Tannlege")
        assertThat(response[0].utbetalinger[0].belop).isEqualTo(10.0)
        assertThat(response[0].utbetalinger[0].fiksDigisosId).isEqualTo(digisosId)
        assertThat(response[0].utbetalinger[0].utbetalingsdato).isEqualTo("2019-08-12")
        assertThat(response[0].utbetalinger[1].tittel).isEqualTo("Nødhjelp")
        assertThat(response[0].utbetalinger[1].belop).isEqualTo(10.0)
        assertThat(response[0].utbetalinger[1].fiksDigisosId).isEqualTo(digisosId)
        assertThat(response[0].utbetalinger[1].utbetalingsdato).isEqualTo("2019-08-10")
    }

    @Test
    fun `Skal returnere response med 1 utbetaling for 2 maneder`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", null, LocalDate.of(2019, 8, 10), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now()),
            Utbetaling("Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Tannlege", null, LocalDate.of(2019, 9, 12), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now())
        )

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotNull
        assertThat(response).hasSize(2)
        assertThat(response[0].ar).isEqualTo(2019)
        assertThat(response[0].maned).isEqualToIgnoringCase("september")
        assertThat(response[0].foersteIManeden).isEqualTo(LocalDate.of(2019, 9, 1))
        assertThat(response[0].utbetalinger).hasSize(1)
        assertThat(response[0].utbetalinger[0].tittel).isEqualTo("Tannlege")
        assertThat(response[0].utbetalinger[0].belop).isEqualTo(10.0)
        assertThat(response[0].utbetalinger[0].fiksDigisosId).isEqualTo(digisosId)
        assertThat(response[0].utbetalinger[0].utbetalingsdato).isEqualTo("2019-09-12")

        assertThat(response[1].ar).isEqualTo(2019)
        assertThat(response[1].maned).isEqualToIgnoringCase("august")
        assertThat(response[1].foersteIManeden).isEqualTo(LocalDate.of(2019, 8, 1))
        assertThat(response[1].utbetalinger).hasSize(1)
        assertThat(response[1].utbetalinger[0].tittel).isEqualTo("Nødhjelp")
        assertThat(response[1].utbetalinger[0].belop).isEqualTo(10.0)
        assertThat(response[1].utbetalinger[0].fiksDigisosId).isEqualTo(digisosId)
        assertThat(response[1].utbetalinger[0].utbetalingsdato).isEqualTo("2019-08-10")
    }

    @Disabled("disabled frem til det blir bekreftet om vilkår skal være med i response")
    @Test
    fun `Skal returnere response med 1 utbetaling med vilkar`() {
        val model = InternalDigisosSoker()
        val now = LocalDateTime.now()
        val vilkar = Vilkar("vilkar1", "tittel", "Skal hoppe", Oppgavestatus.RELEVANT, now, now)
        val utbetaling1 = Utbetaling(
            "referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp",
            null, LocalDate.of(2019, 8, 10), null, null, null, false, null, null, mutableListOf(vilkar), mutableListOf(), LocalDateTime.now()
        )
        model.saker.add(
            Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf(
                    utbetaling1,
                    Utbetaling(
                        "Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Tannlege", null,
                        LocalDate.of(2019, 9, 12), null, null, null, false, null, null, mutableListOf(vilkar), mutableListOf(), LocalDateTime.now()
                    )
                )
            )
        )

        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].utbetalinger).hasSize(2)
    }

    @Disabled("disabled frem til det blir bekreftet om dokumentasjonkrav skal være med i response")
    @Test
    fun `Skal returnere response med 1 utbetaling med dokumentasjonkrav`() {
        val model = InternalDigisosSoker()
        val now = LocalDateTime.now()
        val dokumentasjonkrav = Dokumentasjonkrav(
            dokumentasjonkravId,
            JsonVedlegg.HendelseType.DOKUMENTASJONKRAV,
            "dokumentasjonkrav",
            "tittel",
            "Skal hoppe",
            Oppgavestatus.RELEVANT,
            now,
            LocalDate.now()
        )
        val utbetaling1 = Utbetaling(
            "referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp",
            null, LocalDate.of(2019, 8, 10), null, null, null, false, null, null, mutableListOf(), mutableListOf(dokumentasjonkrav), LocalDateTime.now()
        )
        model.saker.add(
            Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf(utbetaling1)
            )
        )

        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotNull
        assertThat(response).hasSize(1)
        assertThat(response[0].utbetalinger).hasSize(1)
    }

    @Test
    fun `Skal returnere utbetalinger for alle digisosSaker`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling(
                "Sak1", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", null,
                LocalDate.of(2019, 8, 10), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now()
            )
        )

        val model2 = InternalDigisosSoker()
        model2.utbetalinger = mutableListOf(
            Utbetaling(
                "Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.ONE, "Barnehage og SFO", null,
                LocalDate.of(2019, 9, 12), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now()
            )
        )

        val mockDigisosSak2: DigisosSak = mockk()
        val id1 = "some id"
        val id2 = "other id"
        coEvery { mockDigisosSak.fiksDigisosId } returns id1
        coEvery { mockDigisosSak.kommunenummer } returns "kommune1"
        coEvery { mockDigisosSak.sistEndret } returns DateTime.now().millis
        coEvery { mockDigisosSak2.fiksDigisosId } returns id2
        coEvery { mockDigisosSak2.kommunenummer } returns "kommune2"
        coEvery { mockDigisosSak2.sistEndret } returns DateTime.now().millis
        coEvery { eventService.hentAlleUtbetalinger(token, mockDigisosSak) } returns model
        coEvery { eventService.hentAlleUtbetalinger(token, mockDigisosSak2) } returns model2
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak, mockDigisosSak2)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotEmpty
        assertThat(response).hasSize(2)

        assertThat(response[0].ar).isEqualTo(2019)
        assertThat(response[0].maned).isEqualToIgnoringCase("september")
        assertThat(response[0].foersteIManeden).isEqualTo(LocalDate.of(2019, 9, 1))
        assertThat(response[0].utbetalinger).hasSize(1)
        assertThat(response[0].utbetalinger[0].tittel).isEqualTo("Barnehage og SFO")
        assertThat(response[0].utbetalinger[0].belop).isEqualTo(1.0)
        assertThat(response[0].utbetalinger[0].fiksDigisosId).isEqualTo(id2)
        assertThat(response[0].utbetalinger[0].utbetalingsdato).isEqualTo("2019-09-12")

        assertThat(response[1].ar).isEqualTo(2019)
        assertThat(response[1].maned).isEqualToIgnoringCase("august")
        assertThat(response[1].foersteIManeden).isEqualTo(LocalDate.of(2019, 8, 1))
        assertThat(response[1].utbetalinger).hasSize(1)
        assertThat(response[1].utbetalinger[0].tittel).isEqualTo("Nødhjelp")
        assertThat(response[1].utbetalinger[0].belop).isEqualTo(10.0)
        assertThat(response[1].utbetalinger[0].fiksDigisosId).isEqualTo(id1)
        assertThat(response[1].utbetalinger[0].utbetalingsdato).isEqualTo("2019-08-10")
    }

    @Test
    fun `utbetaling uten beskrivelse gir default tittel`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(
            Utbetaling(
                "Sak1", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, null, null,
                LocalDate.of(2019, 8, 10), null, null, null, false, null, null, mutableListOf(), mutableListOf(), LocalDateTime.now()
            )
        )

        coEvery { eventService.hentAlleUtbetalinger(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns listOf(mockDigisosSak)

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isNotEmpty
        assertThat(response).hasSize(1)
        assertThat(response[0].utbetalinger).hasSize(1)
        assertThat(response[0].utbetalinger[0].tittel).isEqualTo(UTBETALING_DEFAULT_TITTEL)
    }

    @Test
    fun `utbetalings hendelse til bruker skal gi kontonummer`() {
        val model = InternalDigisosSoker()
        val kontonummer = "12345678901"
        val utbetalingsreferanse = "0987654321"
        val utbetaling = JsonUtbetaling()
            .withKontonummer(kontonummer)
            .withUtbetalingsreferanse(utbetalingsreferanse)
            .withAnnenMottaker(false)
            .withHendelsestidspunkt(ZonedDateTime.now().toString())
        model.apply(utbetaling)
        assertThat(model.utbetalinger).isNotEmpty
        assertThat(model.utbetalinger).hasSize(1)
        assertThat(model.utbetalinger[0].kontonummer).isEqualTo(kontonummer)
    }

    @Test
    fun `utbetalings hendelse til annen bruker skal ikke gi kontonummer`() {
        val model = InternalDigisosSoker()
        val kontonummer = "12345678901"
        val utbetalingsreferanse = "0987654321"
        val utbetaling = JsonUtbetaling()
            .withKontonummer(kontonummer)
            .withUtbetalingsreferanse(utbetalingsreferanse)
            .withAnnenMottaker(true)
            .withHendelsestidspunkt(ZonedDateTime.now().toString())
        model.apply(utbetaling)
        assertThat(model.utbetalinger).isNotEmpty
        assertThat(model.utbetalinger).hasSize(1)
        assertThat(model.utbetalinger[0].kontonummer).isNull()
    }

    @Test
    fun `utbetalings hendelse nar annen mottaker er null`() {
        val model = InternalDigisosSoker()
        val kontonummer = "12345678901"
        val utbetalingsreferanse = "0987654321"
        val utbetaling = JsonUtbetaling()
            .withKontonummer(kontonummer)
            .withUtbetalingsreferanse(utbetalingsreferanse)
            .withAnnenMottaker(null)
            .withHendelsestidspunkt(ZonedDateTime.now().toString())
        model.apply(utbetaling)
        assertThat(model.utbetalinger).isNotEmpty
        assertThat(model.utbetalinger).hasSize(1)
        assertThat(model.utbetalinger[0].kontonummer).isNull()
    }
}
