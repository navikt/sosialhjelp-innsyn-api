package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.Ettersendelse
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.vedlegg.VedleggHistorikkService.Vedlegg
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

internal class VedleggHistorikkServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()

    private val service = VedleggHistorikkService(fiksClient, dokumentlagerClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonVedleggSpesifikasjon: JsonVedleggSpesifikasjon = mockk()

    @BeforeEach
    internal fun setUp() {
        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV } returns originalSoknad
        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns ettersendelser

        every { mockJsonVedleggSpesifikasjon.vedlegg } returns emptyList()

        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any()) } returns soknadVedleggSpesifikasjon
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_1, any()) } returns ettersendteVedleggSpesifikasjon_1
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_2, any()) } returns ettersendteVedleggSpesifikasjon_2
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_3, any()) } returns ettersendteVedleggSpesifikasjon_3
    }

    @Test
    fun `skal returnere emptylist hvis soknad har null vedlegg og ingen ettersendelser finnes`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any()) } returns mockJsonVedleggSpesifikasjon

        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns emptyList()

        val list = service.hentVedlegg(id)

        assertThat(list).isEmpty()
    }

    @Test
    fun `skal kun returnere soknadens vedlegg hvis ingen ettersendelser finnes`() {
        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns emptyList()

        val list: List<Vedlegg> = service.hentVedlegg(id)

        assertThat(list).hasSize(2)
        assertThat(list[0].type).isEqualTo(dokumenttype)
        assertThat(list[1].type).isEqualTo(dokumenttype)
    }

    @Test
    fun `skal filtrere vekk vedlegg som ikke er LastetOpp`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any()) } returns mockJsonVedleggSpesifikasjon

        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns listOf(
                Ettersendelse(
                        navEksternRefId = "ref 3",
                        vedleggMetadata = vedleggMetadata_ettersendelse_3,
                        vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 42)),
                        timestampSendt = tid_1.toEpochMilli()))

        val list = service.hentVedlegg(id)

        assertThat(list).hasSize(0)
    }

    @Test
    fun `skal kun returne ettersendte vedlegg hvis soknaden ikke har noen vedlegg`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any()) } returns mockJsonVedleggSpesifikasjon

        val list = service.hentVedlegg(id)

        assertThat(list).hasSize(3)
        assertThat(list[0].type).isEqualTo(dokumenttype_2)
        assertThat(list[1].type).isEqualTo(dokumenttype_2)
        assertThat(list[2].type).isEqualTo(dokumenttype_3)
    }

    @Test
    fun `skal hente alle vedlegg for digisosSak`() {
        val list = service.hentVedlegg(id)

        Assertions.assertThat(list).hasSize(5)

        // nano-presisjon lacking
        assertThat(list[0].type).isEqualTo(dokumenttype)
        assertThat(list[0].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_soknad.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[1].type).isEqualTo(dokumenttype)
        assertThat(list[1].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_soknad.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[2].type).isEqualTo(dokumenttype_2)
        assertThat(list[2].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[3].type).isEqualTo(dokumenttype_2)
        assertThat(list[3].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[4].type).isEqualTo(dokumenttype_3)
        assertThat(list[4].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_2.atOffset(ZoneOffset.UTC).toLocalDateTime())
    }
}