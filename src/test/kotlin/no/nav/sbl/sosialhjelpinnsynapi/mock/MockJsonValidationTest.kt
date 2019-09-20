package no.nav.sbl.sosialhjelpinnsynapi.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.digisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.norg.NorgClient
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

internal class DefaultMockResponseTest {

    private val innsynService: InnsynService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val norgClient: NorgClient = mockk(relaxed = true)
    private val fiksClient: FiksClient = mockk(relaxed = true)

    private val eventService = EventService(clientProperties, innsynService, norgClient, fiksClient)

    @Test
    fun `validerer digisosSoker`() {
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns digisosSoker
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null
        every { fiksClient.hentDigisosSak(any(), any()).originalSoknadNAV?.timestampSendt } returns 1L

        assertThatCode { eventService.createModel("123", "token") }.doesNotThrowAnyException()
    }
}