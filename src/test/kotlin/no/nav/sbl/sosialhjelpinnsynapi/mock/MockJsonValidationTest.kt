package no.nav.sbl.sosialhjelpinnsynapi.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.digisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggService
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

internal class DefaultMockResponseTest {

    private val innsynService: InnsynService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val norgClient: NorgClient = mockk(relaxed = true)
    private val vedleggService: VedleggService = mockk()

    private val eventService = EventService(clientProperties, innsynService, vedleggService, norgClient)

    @Test
    fun `validerer digisosSoker`() {
        val mockDigisosSak: DigisosSak = mockk()
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns digisosSoker
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null
        every { mockDigisosSak.fiksDigisosId } returns "123"
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns 1L
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some other id"
        every { mockDigisosSak.ettersendtInfoNAV } returns null

        assertThatCode { eventService.createModel(mockDigisosSak, "token") }.doesNotThrowAnyException()
    }
}