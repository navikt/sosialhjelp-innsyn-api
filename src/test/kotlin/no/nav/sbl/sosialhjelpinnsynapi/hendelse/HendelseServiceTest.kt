package no.nav.sbl.sosialhjelpinnsynapi.hendelse

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.domain.Hendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.InternalDigisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class HendelseServiceTest {

    private val eventService: EventService = mockk()
    private val service = HendelseService(eventService)

    private val tidspunkt = LocalDateTime.now()
    private val tidspunkt2 = LocalDateTime.now().minusHours(10)
    private val tidspunkt3 = LocalDateTime.now().minusHours(9)

    private val tittel = "tittel"
    private val tittel2 = "tittel 2"
    private val tittel3 = "tittel 3"

    private val url = "some url"
    private val url2 = "some url 2"
    private val url3 = "some url 3"

    @BeforeEach
    fun init() {
        clearMocks(eventService)
    }

    @Test
    fun `Should return response with 1 hendelse`() {
        val model = InternalDigisosSoker()
        model.historikk.add(Hendelse(tittel, tidspunkt, url))

        every { eventService.createModel(any()) } returns model

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertThat(hendelser).hasSize(1)
        assertThat(hendelser[0].beskrivelse).isEqualTo(tittel)
        assertThat(hendelser[0].tidspunkt).isEqualTo(tidspunkt.toString())
        assertThat(hendelser[0].filUrl).isEqualTo(url)
    }

    @Test
    fun `Should return response with multiple hendelser`() {
        val model = InternalDigisosSoker()
        model.historikk.addAll(listOf(
                Hendelse(tittel, tidspunkt, url),
                Hendelse(tittel2, tidspunkt2, url2),
                Hendelse(tittel3, tidspunkt3, url3)))

        every { eventService.createModel(any()) } returns model

        val hendelser = service.getHendelserForSoknad("123", "Token")

        assertThat(hendelser).hasSize(3)
    }
}