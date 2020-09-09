package no.nav.sbl.sosialhjelpinnsynapi.mock

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.client.norg.NorgClient
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.config.FeatureToggles
import no.nav.sbl.sosialhjelpinnsynapi.event.EventService
import no.nav.sbl.sosialhjelpinnsynapi.mock.responses.digisosSoker
import no.nav.sbl.sosialhjelpinnsynapi.service.innsyn.InnsynService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sosialhjelp.api.fiks.DigisosSak
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

internal class DefaultMockResponseTest {

    private val innsynService: InnsynService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val norgClient: NorgClient = mockk(relaxed = true)
    private val vedleggService: VedleggService = mockk()
    private val featureToggles: FeatureToggles = mockk()

    private val eventService = EventService(clientProperties, innsynService, vedleggService, norgClient, featureToggles)

    @Test
    fun `validerer digisosSoker`() {
        val mockDigisosSak: DigisosSak = mockk()
        every { innsynService.hentJsonDigisosSoker(any(), any(), any()) } returns digisosSoker
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null
        every { mockDigisosSak.fiksDigisosId } returns "123"
        every { mockDigisosSak.originalSoknadNAV?.timestampSendt } returns 1L
        every { mockDigisosSak.originalSoknadNAV?.navEksternRefId } returns null
        every { mockDigisosSak.digisosSoker?.metadata } returns "some id"
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "some other id"
        every { mockDigisosSak.ettersendtInfoNAV } returns null
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null

        every { featureToggles.vilkarEnabled } returns true
        every { featureToggles.dokumentasjonkravEnabled } returns true

        assertThatCode { eventService.createModel(mockDigisosSak, "token") }.doesNotThrowAnyException()
    }
}