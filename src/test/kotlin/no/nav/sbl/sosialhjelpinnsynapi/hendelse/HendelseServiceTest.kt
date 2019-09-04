package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggForHistorikkService
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggForHistorikkService.Vedlegg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class HendelseServiceTest {

    private val eventService: EventService = mockk()
    private val vedleggForHistorikkService: VedleggForHistorikkService = mockk()
    private val service = HendelseService(eventService, vedleggForHistorikkService)

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

    private val url = "some url"
    private val url2 = "some url 2"
    private val url3 = "some url 3"

    @BeforeEach
    fun init() {
        clearMocks(eventService, vedleggForHistorikkService)
    }

    @Test
    fun `Should return response with 1 hendelse`() {
        val model = InternalDigisosSoker()
        model.historikk.add(Hendelse(tittel_sendt, tidspunkt_sendt, url))

        every { eventService.createModel(any()) } returns model

        every { vedleggForHistorikkService.hentVedlegg(any()) } returns emptyList()

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

        every { eventService.createModel(any()) } returns model

        every { vedleggForHistorikkService.hentVedlegg(any()) } returns emptyList()

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(3)
    }

    @Test
    fun `Should return response with opplastede vedlegg`() {
        val model = InternalDigisosSoker()
        model.historikk.addAll(listOf(
                Hendelse(tittel_sendt, tidspunkt_sendt, url),
                Hendelse(tittel_mottatt, tidspunkt_mottatt, url2),
                Hendelse(tittel3, tidspunkt3, url3)))

        every { eventService.createModel(any()) } returns model

        every { vedleggForHistorikkService.hentVedlegg(any()) } returns listOf(
                Vedlegg(tittel4, tidspunkt4),
                Vedlegg(tittel5, tidspunkt5))

        val hendelser = service.hentHendelser("123", "Token")

        assertThat(hendelser).hasSize(5)

        assertThat(hendelser[3].beskrivelse).contains("Vedlegg lastet opp").contains(tittel4)
        assertThat(hendelser[4].beskrivelse).contains("Vedlegg lastet opp").contains(tittel5)
    }
}