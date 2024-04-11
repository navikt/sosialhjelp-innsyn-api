package no.nav.sosialhjelp.innsyn.digisossak.hendelser

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.HendelseTekstType
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

internal class HendelseServiceTest {
    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val service = HendelseService(eventService, vedleggService, fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()

    companion object {
        private val tidspunkt_sendt = LocalDateTime.now().minusDays(1)
        private val tidspunkt_mottatt = LocalDateTime.now().minusHours(10)
        private val tidspunkt3 = LocalDateTime.now().minusHours(9)
        private val tidspunkt4 = LocalDateTime.now().minusHours(8)
        private val tidspunkt5 = LocalDateTime.now().minusHours(7)

        private const val URL = "some url"
        private const val URL_2 = "some url 2"
        private const val URL_3 = "some url 3"

        private const val DOKUMENTTYPE_1 = "strømregning"
        private const val DOKUMENTTYPE_2 = "tannlegeregning"

        private val DOK_1 = DokumentInfo("tittel 4", "id1", 11)
        private val DOK_2 = DokumentInfo("tittel 5", "id2", 22)
        private val DOK_3 = DokumentInfo("tittel 6", "id3", 33)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()

        coEvery { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockk()
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_sendt.toInstant(ZoneOffset.UTC).toEpochMilli()
        every { mockDigisosSak.kommunenummer } returns "123"
    }

    @Test
    fun `Skal returnere respons med 1 hendelse`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.historikk.add(
                Hendelse(
                    HendelseTekstType.SOKNAD_SEND_TIL_KONTOR,
                    tidspunkt_sendt,
                    UrlResponse(HendelseTekstType.VIS_BREVET_LENKETEKST, URL),
                ),
            )

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(1)
            assertThat(hendelser[0].hendelseType).isEqualTo(HendelseTekstType.SOKNAD_SEND_TIL_KONTOR.name)
            assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt_sendt.toString())
            assertThat(hendelser[0].filUrl?.link).isEqualTo(URL)
        }

    @Test
    fun `Skal returnere respons med flere hendelser`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()
            model.historikk.addAll(
                listOf(
                    Hendelse(
                        HendelseTekstType.SOKNAD_SEND_TIL_KONTOR,
                        tidspunkt_sendt,
                        UrlResponse(HendelseTekstType.VIS_BREVET_LENKETEKST, URL),
                    ),
                    Hendelse(
                        HendelseTekstType.SOKNAD_MOTTATT_MED_KOMMUNENAVN,
                        tidspunkt_mottatt,
                        UrlResponse(HendelseTekstType.VIS_BREVET_LENKETEKST, URL_2),
                    ),
                    Hendelse(
                        HendelseTekstType.SOKNAD_UNDER_BEHANDLING,
                        tidspunkt3,
                        UrlResponse(HendelseTekstType.VIS_BREVET_LENKETEKST, URL_3),
                    ),
                ),
            )

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(3)
        }

    @Test
    fun `Hendelse for opplastet vedlegg`() =
        runTest(timeout = 5.seconds) {
            coEvery { eventService.createModel(any(), any()) } returns InternalDigisosSoker()
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns
                listOf(
                    InternalVedlegg(DOKUMENTTYPE_1, null, null, null, mutableListOf(DOK_1), tidspunkt4, null),
                    InternalVedlegg(DOKUMENTTYPE_2, null, null, null, mutableListOf(DOK_2, DOK_3), tidspunkt5, null),
                )

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(2)

            assertThat(hendelser[0].hendelseType).isEqualTo(HendelseTekstType.ANTALL_SENDTE_VEDLEGG.name)
            assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
            assertThat(hendelser[0].tekstArgument).isEqualTo("1")
            assertThat(hendelser[1].hendelseType).isEqualTo(HendelseTekstType.ANTALL_SENDTE_VEDLEGG.name)
            assertThat(hendelser[1].tidspunkt).isEqualTo(tidspunkt5.toString())
        }

    @Test
    fun `Hendelse for opplastet vedlegg - tom fil-liste skal ikke resultere i hendelse`() =
        runTest(timeout = 5.seconds) {
            coEvery { eventService.createModel(any(), any()) } returns InternalDigisosSoker()
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns
                listOf(
                    InternalVedlegg(DOKUMENTTYPE_2, null, null, null, mutableListOf(), tidspunkt5, null),
                )

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(0)
        }

    @Test
    fun `Hendelse for opplastet vedlegg - samme dokument tilknyttet to saker`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns
                listOf(
                    InternalVedlegg(DOKUMENTTYPE_1, null, null, null, mutableListOf(DOK_1, DOK_2), tidspunkt4, null),
                    InternalVedlegg(DOKUMENTTYPE_2, null, null, null, mutableListOf(DOK_2, DOK_3), tidspunkt5, null),
                )

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(2)

            assertThat(hendelser[0].hendelseType).isEqualTo(HendelseTekstType.ANTALL_SENDTE_VEDLEGG.name)
            assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
            assertThat(hendelser[0].tekstArgument).isEqualTo("2")
            assertThat(hendelser[1].hendelseType).contains(HendelseTekstType.ANTALL_SENDTE_VEDLEGG.name)
            assertThat(hendelser[1].tidspunkt).isEqualTo(tidspunkt5.toString())
            assertThat(hendelser[1].tekstArgument).isEqualTo("2")
        }

    @Test
    fun `Hendelse for opplastet vedlegg - grupperer opplastinger som har samme tidspunkt`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns
                listOf(
                    InternalVedlegg(DOKUMENTTYPE_1, null, null, null, mutableListOf(DOK_1), tidspunkt4, null),
                    InternalVedlegg(DOKUMENTTYPE_2, null, null, null, mutableListOf(DOK_2), tidspunkt4, null),
                )

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(1)

            assertThat(hendelser[0].hendelseType).contains(HendelseTekstType.ANTALL_SENDTE_VEDLEGG.name)
            assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
            assertThat(hendelser[0].tekstArgument).isEqualTo("2")
        }

    @Test
    fun `Hendelse for opplastet vedlegg - grupperer ikke ved millisekund-avvik`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns
                mutableListOf(
                    InternalVedlegg(DOKUMENTTYPE_1, null, null, null, mutableListOf(DOK_1), tidspunkt4, null),
                    InternalVedlegg(DOKUMENTTYPE_2, null, null, null, mutableListOf(DOK_2), tidspunkt4.plus(1, ChronoUnit.MILLIS), null),
                )

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(2)
        }

    @Test
    internal fun `hendelser for utbetalinger - grupper utbetalinger`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()

            val time = LocalDateTime.of(2020, 3, 1, 12, 30, 0)
            model.utbetalinger =
                mutableListOf(
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time,
                    ),
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(4).plusSeconds(59),
                    ),
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(5),
                    ),
                )

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(2)
            val first = hendelser[0]
            assertThat(first.tidspunkt).isEqualTo(time.toString())

            val second = hendelser[1]
            assertThat(second.tidspunkt).isEqualTo(time.plusMinutes(5).toString())
        }

    @Test
    internal fun `hendelser for utbetalinger - utbetalinger med annen status enn ANNULERT blir filtrert bort`() =
        runTest(timeout = 5.seconds) {
            val model = InternalDigisosSoker()

            val time = LocalDateTime.of(2020, 3, 1, 12, 30, 0)
            model.utbetalinger =
                mutableListOf(
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.PLANLAGT_UTBETALING,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time,
                    ),
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.ANNULLERT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(10),
                    ),
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.STOPPET,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(20),
                    ),
                    Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "utbetalt utbetaling",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        stoppetDato = null,
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(30),
                    ),
                )

            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

            val hendelser = service.hentHendelser("123", "Token")

            assertThat(hendelser).hasSize(3)
            assertThat(hendelser[0].tidspunkt).isEqualTo(time.toString())
            assertThat(hendelser[1].tidspunkt).isEqualTo(time.plusMinutes(20).toString())
            assertThat(hendelser[2].tidspunkt).isEqualTo(time.plusMinutes(30).toString())
        }
}
