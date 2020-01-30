package no.nav.sbl.sosialhjelpinnsynapi.utbetalinger

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.digisos.soker.hendelse.JsonUtbetaling
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.event.apply
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class UtbetalingerServiceTest {
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()

    private val service = UtbetalingerService(eventService, fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()

    private val token = "token"

    private val digisosId = "some id"

    private val tittel = "tittel"
    private val referanse = "referanse"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { mockDigisosSak.fiksDigisosId } returns digisosId
        every { mockDigisosSak.sistEndret } returns DateTime.now().millis
    }

    @Test
    fun `Skal returnere emptyList hvis soker ikke har noen digisosSaker`() {
        val model = InternalDigisosSoker()
        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentAlleDigisosSaker(any()) } returns emptyList()

        val response: List<UtbetalingerResponse> = service.hentUtbetalinger(token, 6)

        assertThat(response).isEmpty()
    }


    @Test
    fun `Skal returnere response med 1 utbetaling`() {
        val model = InternalDigisosSoker()
        model.utbetalinger = mutableListOf(Utbetaling(
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
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf()
                ))

        every { eventService.hentAlleUtbetalinger(any(), any()) } returns model
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
                Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", null, LocalDate.of(2019, 8, 10), null, null, null, null, null, mutableListOf(), mutableListOf()),
                Utbetaling("Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Tannlege", null, LocalDate.of(2019, 8, 12), null, null, null, null, null, mutableListOf(), mutableListOf())
        )

        every { eventService.hentAlleUtbetalinger(any(), any()) } returns model
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
                Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", null, LocalDate.of(2019, 8, 10), null, null, null, null, null, mutableListOf(), mutableListOf()),
                Utbetaling("Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Tannlege", null, LocalDate.of(2019, 9, 12), null, null, null, null, null, mutableListOf(), mutableListOf())
        )

        every { eventService.hentAlleUtbetalinger(any(), any()) } returns model
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
        val vilkar = Vilkar("vilkar1", mutableListOf(), "Skal hoppe", false)
        val utbetaling1 = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp",
                null, LocalDate.of(2019, 8, 10), null, null, null, null, null, mutableListOf(vilkar), mutableListOf())
        vilkar.utbetalinger.add(utbetaling1)
        model.saker.add(Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf(
                        utbetaling1,
                        Utbetaling("Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Tannlege", null,
                                LocalDate.of(2019, 9, 12), null, null, null, null, null, mutableListOf(vilkar), mutableListOf())
                ),
                vilkar = mutableListOf(vilkar),
                dokumentasjonkrav = mutableListOf()
        ))

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
        val dokumentasjonkrav = Dokumentasjonkrav("dokumentasjonskrav", mutableListOf(), "Skal hoppe", false)
        val utbetaling1 = Utbetaling("referanse", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp",
                null, LocalDate.of(2019, 8, 10), null, null, null, null, null, mutableListOf(), mutableListOf(dokumentasjonkrav))
        dokumentasjonkrav.utbetalinger.add(utbetaling1)
        model.saker.add(Sak(
                referanse = referanse,
                saksStatus = SaksStatus.UNDER_BEHANDLING,
                tittel = tittel,
                vedtak = mutableListOf(),
                utbetalinger = mutableListOf(utbetaling1),
                vilkar = mutableListOf(),
                dokumentasjonkrav = mutableListOf(dokumentasjonkrav)
        ))

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
                Utbetaling("Sak1", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, "Nødhjelp", null,
                        LocalDate.of(2019, 8, 10), null, null, null, null, null, mutableListOf(), mutableListOf())
        )

        val model2 = InternalDigisosSoker()
        model2.utbetalinger = mutableListOf(
                Utbetaling("Sak2", UtbetalingsStatus.UTBETALT, BigDecimal.ONE, "Barnehage og SFO", null,
                        LocalDate.of(2019, 9, 12), null, null, null, null, null, mutableListOf(), mutableListOf())
        )

        val mockDigisosSak2: DigisosSak = mockk()
        val id1 = "some id"
        val id2 = "other id"
        every { mockDigisosSak.fiksDigisosId } returns id1
        every { mockDigisosSak.sistEndret } returns DateTime.now().millis
        every { mockDigisosSak2.fiksDigisosId } returns id2
        every { mockDigisosSak2.sistEndret } returns DateTime.now().millis
        every { eventService.hentAlleUtbetalinger(token, mockDigisosSak) } returns model
        every { eventService.hentAlleUtbetalinger(token, mockDigisosSak2) } returns model2
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
                        Utbetaling("Sak1", UtbetalingsStatus.UTBETALT, BigDecimal.TEN, null, null,
                                LocalDate.of(2019, 8, 10), null, null, null, null, null, mutableListOf(), mutableListOf()))

        every { eventService.hentAlleUtbetalinger(any(), any()) } returns model
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
        utbetaling.kontonummer = kontonummer
        utbetaling.utbetalingsreferanse = utbetalingsreferanse
        utbetaling.annenMottaker = false
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
        utbetaling.kontonummer = kontonummer
        utbetaling.utbetalingsreferanse = utbetalingsreferanse
        utbetaling.annenMottaker = true
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
        utbetaling.kontonummer = kontonummer
        utbetaling.utbetalingsreferanse = utbetalingsreferanse
        model.apply(utbetaling)
        assertThat(model.utbetalinger).isNotEmpty
        assertThat(model.utbetalinger).hasSize(1)
        assertThat(model.utbetalinger[0].kontonummer).isNull()
    }
}
