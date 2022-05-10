package no.nav.sosialhjelp.innsyn.service.originalsoknad

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.domain.OrginalJsonSoknadResponse
import no.nav.sosialhjelp.innsyn.service.innsyn.InnsynService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OrginalSoknadServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val innsynService: InnsynService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    private val service = OrginalSoknadService(
        fiksClient,
        innsynService,
        clientProperties
    )

    private val mockDigisosSak: DigisosSak = mockk()
    private val orginalSoknadNAV: OriginalSoknadNAV = mockk()
    private val mockJsonSoknad: JsonSoknad = mockk()

    @Test
    fun `sjekk at hentOrginalJsonSoknad returnerer null hvis hentOrginalSoknad returnerer null`() {

        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "metadata"
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any(), any()) } returns null

        assertThat(service.hentOrginalJsonSoknad("1234", "token")).isEqualTo(null)
    }

    @Test
    fun `sjekk at hentOrginalJsonSoknad returnerer en gyldig JsonSoknad hvis hentOrginalSoknad gir en gyldig JsonSoknad`() {

        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.metadata } returns "metadata"
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any(), any()) } returns mockJsonSoknad

        assertThat(service.hentOrginalJsonSoknad("1234", "token")).isEqualTo(OrginalJsonSoknadResponse(mockJsonSoknad))
    }

    @Test
    fun `skal returnere pdf-link fra dokumentlager`() {
        val dokumentlagerId = "id"
        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns dokumentlagerId
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak

        val response = service.hentOrginalSoknadPdfLink("1234", "token")
        assertThat(response?.orginalSoknadPdfLink).contains(dokumentlagerId)
    }

    @Test
    fun `skal returnere null hvis dokumentlagerid ikke finnes`() {
        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId } returns null
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak

        val response = service.hentOrginalSoknadPdfLink("1234", "token")
        assertThat(response).isNull()
    }
}
