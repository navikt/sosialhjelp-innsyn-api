package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService.InternalVedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

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
    private val tittel4 = "tittel 4"
    private val tittel5 = "tittel 5"
    private val tittel6 = "tittel 6"

    private val url = "some url"
    private val url2 = "some url 2"
    private val url3 = "some url 3"

    private val dokumenttype_1 = "strømregning"
    private val dokumenttype_2 = "tannlegeregning"

    @BeforeEach
    fun init() {
        clearMocks(eventService, vedleggService, fiksClient)

        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.ettersendtInfoNAV } returns mockk()
        every { mockDigisosSak.originalSoknadNAV!!.timestampSendt } returns tidspunkt_sendt.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    @Test
    fun `Should return response with 1 hendelse`() {
        val model = InternalDigisosSoker()
        model.historikk.add(Hendelse(tittel_sendt, tidspunkt_sendt, url))

        every { eventService.createModel(any(), any()) } returns model

        every { vedleggService.hentEttersendteVedlegg(any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(1)
        assertThat(hendelser[0].beskrivelse).isEqualTo(tittel_sendt)
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt_sendt.toString())
        assertThat(hendelser[0].filUrl).isEqualTo(url)
    }

    @Test
    fun `Should return response with multiple hendelser`() {
        val model = InternalDigisosSoker()
        model.historikk.addAll(listOf(
                Hendelse(tittel_sendt, tidspunkt_sendt, url),
                Hendelse(tittel_mottatt, tidspunkt_mottatt, url2),
                Hendelse(tittel3, tidspunkt3, url3)))

        every { eventService.createModel(any(), any()) } returns model

        every { vedleggService.hentEttersendteVedlegg(any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(3)
    }

    @Test
    fun `Should return response with opplastede vedlegg`() {
        every { eventService.createModel(any(), any()) } returns InternalDigisosSoker()

        every { vedleggService.hentEttersendteVedlegg(any()) } returns listOf(
                InternalVedlegg(dokumenttype_1, null, listOf(DokumentInfo(tittel4, "id1", 42)), tidspunkt4),
                InternalVedlegg(dokumenttype_2, null, listOf(DokumentInfo(tittel5, "id2", 11), DokumentInfo(tittel6, "id3", 22)), tidspunkt5))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)

        assertThat(hendelser[0].beskrivelse).contains(dokumenttype_1, tittel4)
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
        assertThat(hendelser[1].beskrivelse).contains(dokumenttype_2, tittel5, tittel6)
        assertThat(hendelser[1].tidspunkt).isEqualTo(tidspunkt5.toString())
    }

    @Test
    fun `Should return response with opplastede vedlegg  - samme dokument tilknyttet to saker`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any(), any()) } returns model

        every { vedleggService.hentEttersendteVedlegg(any()) } returns listOf(
                InternalVedlegg(dokumenttype_1, null, listOf(DokumentInfo(tittel4, "id4", 4), DokumentInfo(tittel5, "id5", 5)), tidspunkt4),
                InternalVedlegg(dokumenttype_2, null, listOf(DokumentInfo(tittel5, "id5", 5), DokumentInfo(tittel6, "id6", 6)), tidspunkt5))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(2)

        assertThat(hendelser[0].beskrivelse).contains(dokumenttype_1, tittel4, tittel5)
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt4.toString())
        assertThat(hendelser[1].beskrivelse).contains(dokumenttype_2, tittel5, tittel6)
        assertThat(hendelser[1].tidspunkt).isEqualTo(tidspunkt5.toString())
    }
}