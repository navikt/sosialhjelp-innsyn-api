package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.DokumentInfo
import no.nav.sbl.sosialhjelpinnsynapi.domain.Ettersendelse
import no.nav.sbl.sosialhjelpinnsynapi.domain.OriginalSoknadNAV
import no.nav.sbl.sosialhjelpinnsynapi.fiks.DokumentlagerClient
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal class VedleggServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)

    private val service = VedleggService(fiksClient, dokumentlagerClient, clientProperties)

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

        val list = service.hentAlleVedlegg(id)

        assertThat(list).isEmpty()
    }

    @Test
    fun `skal kun returnere soknadens vedlegg hvis ingen ettersendelser finnes`() {
        every { mockDigisosSak.ettersendtInfoNAV.ettersendelser } returns emptyList()

        val list = service.hentAlleVedlegg(id)

        assertThat(list).hasSize(2)
        assertThat(list[0].filnavn).isEqualTo(soknad_filnavn_1)
        assertThat(list[1].filnavn).isEqualTo(soknad_filnavn_2)
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

        val list = service.hentAlleVedlegg(id)

        assertThat(list).hasSize(0)
    }

    @Test
    fun `skal kun returne ettersendte vedlegg hvis soknaden ikke har noen vedlegg`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any()) } returns mockJsonVedleggSpesifikasjon

        val list = service.hentAlleVedlegg(id)

        assertThat(list).hasSize(3)
        assertThat(list[0].filnavn).isEqualTo(ettersendelse_filnavn_1)
        assertThat(list[1].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[2].filnavn).isEqualTo(ettersendelse_filnavn_3)
    }

    @Test
    fun `skal hente alle vedlegg for digisosSak`() {
        val list = service.hentAlleVedlegg(id)

        assertThat(list).hasSize(5)

        // nano-presisjon lacking
        assertThat(list[0].filnavn).isEqualTo(soknad_filnavn_1)
        assertThat(list[0].storrelse).isEqualTo(1337)
        assertThat(list[0].url).contains(dokumentlagerId_4)
        assertThat(list[0].beskrivelse).isEqualTo(dokumenttype)
        assertThat(list[0].datoLagtTil).isEqualToIgnoringNanos(tid_soknad.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[1].filnavn).isEqualTo(soknad_filnavn_2)
        assertThat(list[1].storrelse).isEqualTo(1337)
        assertThat(list[1].url).contains(dokumentlagerId_5)
        assertThat(list[1].beskrivelse).isEqualTo(dokumenttype)
        assertThat(list[1].datoLagtTil).isEqualToIgnoringNanos(tid_soknad.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[2].filnavn).isEqualTo(ettersendelse_filnavn_1)
        assertThat(list[2].storrelse).isEqualTo(42)
        assertThat(list[2].url).contains(dokumentlagerId_1)
        assertThat(list[2].beskrivelse).isEqualTo(dokumenttype_2)
        assertThat(list[2].datoLagtTil).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[3].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[3].storrelse).isEqualTo(42)
        assertThat(list[3].url).contains(dokumentlagerId_2)
        assertThat(list[3].beskrivelse).isEqualTo(dokumenttype_2)
        assertThat(list[3].datoLagtTil).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[4].filnavn).isEqualTo(ettersendelse_filnavn_3)
        assertThat(list[4].storrelse).isEqualTo(42)
        assertThat(list[4].url).contains(dokumentlagerId_3)
        assertThat(list[4].beskrivelse).isEqualTo(dokumenttype_3)
        assertThat(list[4].datoLagtTil).isEqualToIgnoringNanos(tid_2.atOffset(ZoneOffset.UTC).toLocalDateTime())
    }
}

internal val id = "123"

internal val ettersendelse_filnavn_1 = "filnavn.pdf"
internal val ettersendelse_filnavn_2 = "navn på fil.ocr"
internal val ettersendelse_filnavn_3 = "denne filens navn.jpg"
internal val soknad_filnavn_1 = "originalSoknadVedlegg.png"
internal val soknad_filnavn_2 = "originalSoknadVedlegg_2.exe"

internal val dokumentlagerId_1 = "9999"
internal val dokumentlagerId_2 = "7777"
internal val dokumentlagerId_3 = "5555"
internal val dokumentlagerId_4 = "3333"
internal val dokumentlagerId_5 = "1111"

internal val dokumenttype = "type"
internal val dokumenttype_2 = "type 2"
internal val dokumenttype_3 = "type 3"

internal val tid_1 = Instant.now()
internal val tid_2 = Instant.now().minus(2, ChronoUnit.DAYS)
internal val tid_soknad = Instant.now().minus(14, ChronoUnit.DAYS)

internal val vedleggMetadata_ettersendelse_1 = "vedlegg metadata 1"
internal val vedleggMetadata_ettersendelse_2 = "vedlegg metadata 2"
internal val vedleggMetadata_ettersendelse_3 = "vedlegg metadata 3"
internal val vedleggMetadata_soknad = "vedlegg metadata soknad"

internal val ettersendelser = listOf(
        Ettersendelse(
                navEksternRefId = "ref 1",
                vedleggMetadata = vedleggMetadata_ettersendelse_1,
                vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 42), DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_2, 42)),
                timestampSendt = tid_1.toEpochMilli()),
        Ettersendelse(
                navEksternRefId = "ref 2",
                vedleggMetadata = vedleggMetadata_ettersendelse_2,
                vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_3, dokumentlagerId_3, 42)),
                timestampSendt = tid_2.toEpochMilli())
)

internal val originalSoknad = OriginalSoknadNAV(
        navEksternRefId = "123",
        metadata = "metadata",
        vedleggMetadata = vedleggMetadata_soknad,
        soknadDokument = mockk(),
        vedlegg = listOf(DokumentInfo(soknad_filnavn_1, dokumentlagerId_4, 1337), DokumentInfo(soknad_filnavn_2, dokumentlagerId_5, 1337)),
        timestampSendt = tid_soknad.toEpochMilli()
)

internal val soknadVedleggSpesifikasjon = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(soknad_filnavn_1).withSha512("1234fasd")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype),
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(soknad_filnavn_2).withSha512("sfg234")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype)
        ))

internal val ettersendteVedleggSpesifikasjon_1 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_1).withSha512("g25b3")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_2),
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("4avc65a8")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_2)
        ))

internal val ettersendteVedleggSpesifikasjon_2 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_3).withSha512("aadsfwr")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_3)
        ))

internal val ettersendteVedleggSpesifikasjon_3 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_3).withSha512("aadsfwr")))
                        .withStatus("VedleggAlleredeSendt")
                        .withType(dokumenttype_3)
        ))