package no.nav.sosialhjelp.innsyn.service.hendelse

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.Unleash
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.client.unleash.VILKAR_ENABLED
import no.nav.sosialhjelp.innsyn.common.VIS_BREVET
import no.nav.sosialhjelp.innsyn.domain.Hendelse
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.domain.Oppgavestatus
import no.nav.sosialhjelp.innsyn.domain.Sak
import no.nav.sosialhjelp.innsyn.domain.SaksStatus
import no.nav.sosialhjelp.innsyn.domain.UrlResponse
import no.nav.sosialhjelp.innsyn.domain.Utbetaling
import no.nav.sosialhjelp.innsyn.domain.UtbetalingsStatus
import no.nav.sosialhjelp.innsyn.domain.Vilkar
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.service.vedlegg.InternalVedlegg
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal class HendelseServiceTest {

    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val unleashClient: Unleash = mockk()
    private val service = HendelseService(eventService, vedleggService, fiksClient, unleashClient)

    private val mockDigisosSak: DigisosSak = mockk()

    private val tidspunkt_sendt = LocalDateTime.now().minusDays(1)
    private val tidspunkt_mottatt = LocalDateTime.now().minusHours(10)
    private val tidspunkt3 = LocalDateTime.now().minusHours(9)
    private val tidspunkt4 = LocalDateTime.now().minusHours(8)
    private val tidspunkt5 = LocalDateTime.now().minusHours(7)

    private val tittel_sendt = "søknad sendt"
    private val tittel_mottatt = "søknad mottatt"
    private val tittel3 = "tittel 3"

    private val url = "some url"
    private val url2 = "some url 2"
    private val url3 = "some url 3"

    private val dokumenttype_1 = "strømregning"
    private val dokumenttype_2 = "tannlegeregning"

    private val dok1 = DokumentInfo("tittel 4", "id1", 11)
    private val dok2 = DokumentInfo("tittel 5", "id2", 22)
    private val dok3 = DokumentInfo("tittel 6", "id3", 33)

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockk()
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_sendt.toInstant(ZoneOffset.UTC).toEpochMilli()
        every { unleashClient.isEnabled(VILKAR_ENABLED, false) } returns true
    }

    @Test
    fun `Skal returnere respons med 1 hendelse`() {
        val model = InternalDigisosSoker()
        model.historikk.add(Hendelse(tittel_sendt, tidspunkt_sendt, UrlResponse(VIS_BREVET, url)))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(1)
        assertThat(hendelser[0].beskrivelse).isEqualTo(tittel_sendt)
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt_sendt.toString())
        assertThat(hendelser[0].filUrl?.link).isEqualTo(url)
    }

    @Test
    fun `Skal returnere respons med flere hendelser`() {
        val model = InternalDigisosSoker()
        model.historikk.addAll(listOf(
                Hendelse(tittel_sendt, tidspunkt_sendt, UrlResponse(VIS_BREVET, url)),
                Hendelse(tittel_mottatt, tidspunkt_mottatt, UrlResponse(VIS_BREVET, url2)),
                Hendelse(tittel3, tidspunkt3, UrlResponse(VIS_BREVET, url3))))

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(3)
    }

    @Test
    fun `Hendelse for opplastet vedlegg`() {
        every { eventService.createModel(any(), any()) } returns InternalDigisosSoker()
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
                InternalVedlegg(dokumenttype_1, null, listOf(dok1), tidspunkt4),
                InternalVedlegg(dokumenttype_2, null, listOf(dok2, dok3), tidspunkt5))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)

        assertThat(hendelser[0].beskrivelse).contains("Du har sendt 1 vedlegg til NAV")
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
        assertThat(hendelser[1].beskrivelse).contains("Du har sendt 2 vedlegg til NAV")
        assertThat(hendelser[1].tidspunkt).isEqualTo(tidspunkt5.toString())
    }

    @Test
    fun `Hendelse for opplastet vedlegg - tom fil-liste skal ikke resultere i hendelse`() {
        every { eventService.createModel(any(), any()) } returns InternalDigisosSoker()
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
                InternalVedlegg(dokumenttype_2, null, emptyList(), tidspunkt5))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(0)
    }

    @Test
    fun `Hendelse for opplastet vedlegg - samme dokument tilknyttet to saker`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
                InternalVedlegg(dokumenttype_1, null, listOf(dok1, dok2), tidspunkt4),
                InternalVedlegg(dokumenttype_2, null, listOf(dok2, dok3), tidspunkt5))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)

        assertThat(hendelser[0].beskrivelse).contains("Du har sendt 2 vedlegg til NAV")
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
        assertThat(hendelser[1].beskrivelse).contains("Du har sendt 2 vedlegg til NAV")
        assertThat(hendelser[1].tidspunkt).isEqualTo(tidspunkt5.toString())
    }

    @Test
    fun `Hendelse for opplastet vedlegg - grupperer opplastinger som har samme tidspunkt`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
                InternalVedlegg(dokumenttype_1, null, listOf(dok1), tidspunkt4),
                InternalVedlegg(dokumenttype_2, null, listOf(dok2), tidspunkt4))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(1)

        assertThat(hendelser[0].beskrivelse).contains("Du har sendt 2 vedlegg til NAV")
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
    }

    @Test
    fun `Hendelse for opplastet vedlegg - grupperer ikke ved millisekund-avvik`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns listOf(
                InternalVedlegg(dokumenttype_1, null, listOf(dok1), tidspunkt4),
                InternalVedlegg(dokumenttype_2, null, listOf(dok2), tidspunkt4.plus(1, ChronoUnit.MILLIS)))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)
    }

    @Test
    internal fun `hendelser for vilkar - grupper vilkar`() {
        val model = InternalDigisosSoker()

        val time = LocalDateTime.of(2020, 3, 1, 12, 30, 1)
        model.saker = mutableListOf(
                Sak(
                        referanse = "saksref",
                        saksStatus = SaksStatus.UNDER_BEHANDLING,
                        tittel = "tittel",
                        vedtak = mutableListOf(),
                        utbetalinger = mutableListOf(
                                Utbetaling(
                                        referanse = "utbetalref",
                                        status = UtbetalingsStatus.UTBETALT,
                                        belop = BigDecimal.valueOf(1337.0),
                                        beskrivelse = "beskrivelse",
                                        forfallsDato = null,
                                        utbetalingsDato = LocalDate.now(),
                                        fom = null,
                                        tom = null,
                                        mottaker = "mottaker",
                                        annenMottaker = false,
                                        kontonummer = "kontonummer",
                                        utbetalingsmetode = "utbetalingsmetode",
                                        vilkar = mutableListOf(
                                                Vilkar("ref1", "tittel", "beskrivelse", Oppgavestatus.RELEVANT, time, time),
                                                Vilkar("ref2", "tittel", "beskrivelse2", Oppgavestatus.RELEVANT, time, time.plusSeconds(28)),
                                                Vilkar("ref3", "tittel", "beskrivelse3", Oppgavestatus.RELEVANT, time, time.plusMinutes(5))),
                                        dokumentasjonkrav = mutableListOf(),
                                        datoHendelse = time
                                )
                        )
                )
        )

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)
        val first = hendelser[0]
        assertThat(first.tidspunkt).isEqualTo(time.toString())

        val second = hendelser[1]
        assertThat(second.tidspunkt).isEqualTo(time.plusMinutes(5).toString())
    }

    @Test
    internal fun `hendelser for utbetalinger - grupper utbetalinger`() {
        val model = InternalDigisosSoker()

        val time = LocalDateTime.of(2020, 3, 1, 12, 30, 0)
        model.utbetalinger = mutableListOf(
                Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time
                ),
                Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(4).plusSeconds(59)
                ),
                Utbetaling(
                        referanse = "utbetalref",
                        status = UtbetalingsStatus.UTBETALT,
                        belop = BigDecimal.valueOf(1337.0),
                        beskrivelse = "beskrivelse",
                        forfallsDato = null,
                        utbetalingsDato = LocalDate.now(),
                        fom = null,
                        tom = null,
                        mottaker = "mottaker",
                        annenMottaker = false,
                        kontonummer = "kontonummer",
                        utbetalingsmetode = "utbetalingsmetode",
                        vilkar = mutableListOf(),
                        dokumentasjonkrav = mutableListOf(),
                        datoHendelse = time.plusMinutes(5)
                )
        )

        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentEttersendteVedlegg(any(), any(), any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)
        val first = hendelser[0]
        assertThat(first.tidspunkt).isEqualTo(time.toString())

        val second = hendelser[1]
        assertThat(second.tidspunkt).isEqualTo(time.plusMinutes(5).toString())
    }
}