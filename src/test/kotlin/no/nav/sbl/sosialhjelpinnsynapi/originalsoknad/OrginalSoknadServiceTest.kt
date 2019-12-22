package no.nav.sbl.sosialhjelpinnsynapi.originalsoknad

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.soknad.JsonSoknad
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.OrginalJsonSoknadResponse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.innsyn.InnsynService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OrginalSoknadServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val innsynService: InnsynService = mockk()
    private val clientProperties: no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties = mockk(relaxed = true)

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
        every { mockDigisosSak.originalSoknadNAV?.metadata} returns "metadata"
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns null

        assertThat(service.hentOrginalJsonSoknad("1234", "token")).isEqualTo(null)
    }

    @Test
    fun `sjekk at hentOrginalJsonSoknad returnerer en gyldig JsonSoknad hvis hentOrginalSoknad gir en gyldig JsonSoknad`() {

        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.metadata} returns "metadata"
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { innsynService.hentOriginalSoknad(any(), any(), any()) } returns mockJsonSoknad

        assertThat(service.hentOrginalJsonSoknad("1234", "token")).isEqualTo(OrginalJsonSoknadResponse(mockJsonSoknad))
    }

    @Test
    fun `skal returnere pdf-link fra dokumentlager`() {
        val dokumentlagerId = "id"
        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId} returns dokumentlagerId
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak

        val response = service.hentOrginalSoknadPdfLink("1234", "token")
        assertThat(response?.orginalSoknadPdfLink).contains(dokumentlagerId)
    }

    @Test
    fun `skal returnere null hvis dokumentlagerid ikke finnes`() {
        every { mockDigisosSak.originalSoknadNAV } returns orginalSoknadNAV
        every { mockDigisosSak.originalSoknadNAV?.soknadDokument?.dokumentlagerDokumentId} returns null
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak

        val response = service.hentOrginalSoknadPdfLink("1234", "token")
        assertThat(response).isNull()
    }
}