package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.*
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal class HendelseServiceTest {

    private val eventService: EventService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val service = HendelseService(eventService, vedleggService, fiksClient)

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
        clearMocks(eventService, vedleggService, fiksClient)

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockk()
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns tidspunkt_sendt.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    @Test
    fun `Skal returnere respons med 1 hendelse`() {
        val model = InternalDigisosSoker()
        model.historikk.add(Hendelse(tittel_sendt, tidspunkt_sendt, UrlResponse("Vis brevet", url)))

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
                Hendelse(tittel_sendt, tidspunkt_sendt, UrlResponse("Vis brevet", url)),
                Hendelse(tittel_mottatt, tidspunkt_mottatt, UrlResponse("Vis brevet", url2)),
                Hendelse(tittel3, tidspunkt3, UrlResponse("Vis brevet", url3))))

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
}