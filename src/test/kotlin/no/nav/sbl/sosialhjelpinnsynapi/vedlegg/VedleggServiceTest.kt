package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
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

    private val service = VedleggService(fiksClient, dokumentlagerClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonVedleggSpesifikasjon: JsonVedleggSpesifikasjon = mockk()

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()

        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV } returns originalSoknad
        every { mockDigisosSak.ettersendtInfoNAV!!.ettersendelser } returns ettersendelser

        every { mockJsonVedleggSpesifikasjon.vedlegg } returns emptyList()

        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any(), "token") } returns soknadVedleggSpesifikasjon
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_1, any(), "token") } returns ettersendteVedleggSpesifikasjon_1
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_2, any(), "token") } returns ettersendteVedleggSpesifikasjon_2
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_3, any(), "token") } returns ettersendteVedleggSpesifikasjon_3
        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_4, any(), "token") } returns ettersendteVedleggSpesifikasjon_4
    }

    @Test
    fun `skal returnere emptylist hvis soknad har null vedlegg og ingen ettersendelser finnes`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any(), "token") } returns mockJsonVedleggSpesifikasjon

        every { mockDigisosSak.ettersendtInfoNAV!!.ettersendelser } returns emptyList()

        val list = service.hentAlleVedlegg(id, "token")

        assertThat(list).isEmpty()
    }

    @Test
    fun `skal kun returnere soknadens vedlegg hvis ingen ettersendelser finnes`() {
        every { mockDigisosSak.ettersendtInfoNAV!!.ettersendelser } returns emptyList()

        val list = service.hentAlleVedlegg(id, "token")

        assertThat(list).hasSize(2)
        assertThat(list[0].type).isEqualTo(dokumenttype)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(soknad_filnavn_1)
        assertThat(list[1].type).isEqualTo(dokumenttype_2)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(soknad_filnavn_2)
    }

    @Test
    fun `skal filtrere vekk vedlegg som ikke er LastetOpp`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any(), "token") } returns mockJsonVedleggSpesifikasjon

        every { mockDigisosSak.ettersendtInfoNAV!!.ettersendelser } returns listOf(
                Ettersendelse(
                        navEksternRefId = "ref 3",
                        vedleggMetadata = vedleggMetadata_ettersendelse_3,
                        vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 42)),
                        timestampSendt = tid_1.toEpochMilli()))

        val list = service.hentAlleVedlegg(id, "token")

        assertThat(list).hasSize(0)
    }

    @Test
    fun `skal kun returne ettersendte vedlegg hvis soknaden ikke har noen vedlegg`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any(), "token") } returns mockJsonVedleggSpesifikasjon

        val list = service.hentAlleVedlegg(id, "token")

        assertThat(list).hasSize(4)
        assertThat(list[0].type).isEqualTo(dokumenttype_3)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_1)

        assertThat(list[1].type).isEqualTo(dokumenttype_4)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_2)

        assertThat(list[2].type).isEqualTo(dokumenttype_3)
        assertThat(list[2].dokumentInfoList).hasSize(2)
        assertThat(list[2].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_3)
        assertThat(list[2].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_4)

        assertThat(list[3].type).isEqualTo(dokumenttype_3)
        assertThat(list[3].dokumentInfoList).hasSize(2)
        assertThat(list[3].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_4)
    }

    @Test
    fun `skal hente alle vedlegg for digisosSak`() {
        val list = service.hentAlleVedlegg(id, "token")

        assertThat(list).hasSize(6)

        // nano-presisjon lacking
        assertThat(list[0].type).isEqualTo(dokumenttype)
        assertThat(list[0].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_soknad.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[1].type).isEqualTo(dokumenttype_2)
        assertThat(list[1].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_soknad.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[2].type).isEqualTo(dokumenttype_3)
        assertThat(list[2].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[3].type).isEqualTo(dokumenttype_4)
        assertThat(list[3].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_1.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[4].type).isEqualTo(dokumenttype_3)
        assertThat(list[4].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_2.atOffset(ZoneOffset.UTC).toLocalDateTime())

        assertThat(list[5].type).isEqualTo(dokumenttype_3)
        assertThat(list[5].tidspunktLastetOpp).isEqualToIgnoringNanos(tid_2.atOffset(ZoneOffset.UTC).toLocalDateTime())
    }

    @Test
    fun `like filnavn i DokumentInfoList vil resultere i at de returneres for hver JsonFil med samme filnavn`() {
        every { dokumentlagerClient.hentDokument(vedleggMetadata_soknad, any(), "token") } returns mockJsonVedleggSpesifikasjon

        every { dokumentlagerClient.hentDokument(vedleggMetadata_ettersendelse_5, any(), "token") } returns
                JsonVedleggSpesifikasjon()
                        .withVedlegg(listOf(
                                JsonVedlegg()
                                        .withFiler(listOf(
                                                JsonFiler().withFilnavn(ettersendelse_filnavn_1).withSha512("1231231"),
                                                JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("adfgbjn")))
                                        .withStatus("LastetOpp")
                                        .withType(dokumenttype_3),
                                JsonVedlegg()
                                        .withFiler(listOf(
                                                JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("aasdcx"),
                                                JsonFiler().withFilnavn(ettersendelse_filnavn_4).withSha512("qweqqa")))
                                        .withStatus("LastetOpp")
                                        .withType(dokumenttype_4)
                        ))

        every { mockDigisosSak.ettersendtInfoNAV!!.ettersendelser } returns listOf(
                Ettersendelse(
                        navEksternRefId = "ref 3",
                        vedleggMetadata = vedleggMetadata_ettersendelse_5,
                        vedlegg = listOf(
                                DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 1),
                                DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_2, 2), // samme filnavn
                                DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_3, 3), // samme filnavn
                                DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_4, 4)),
                        timestampSendt = tid_1.toEpochMilli()))

        val list = service.hentAlleVedlegg(id, "token")

        assertThat(list).hasSize(2)

        assertThat(list[0].dokumentInfoList).hasSize(3)
        assertThat(list[0].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[0].dokumentInfoList[1].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_2)
        assertThat(list[0].dokumentInfoList[2].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[0].dokumentInfoList[2].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_3)

        assertThat(list[1].dokumentInfoList).hasSize(3)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[1].dokumentInfoList[0].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_2)
        assertThat(list[1].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[1].dokumentInfoList[1].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_3)
    }
}

private const val id = "123"

private const val ettersendelse_filnavn_1 = "filnavn.pdf"
private const val ettersendelse_filnavn_2 = "navn på fil.ocr"
private const val ettersendelse_filnavn_3 = "denne filens navn.jpg"
private const val ettersendelse_filnavn_4 = "gif.jpg"
private const val soknad_filnavn_1 = "originalSoknadVedlegg.png"
private const val soknad_filnavn_2 = "originalSoknadVedlegg_2.exe"

private const val dokumentlagerId_1 = "9999"
private const val dokumentlagerId_2 = "7777"
private const val dokumentlagerId_3 = "5555"
private const val dokumentlagerId_4 = "3333"
private const val dokumentlagerId_soknad_1 = "1111"
private const val dokumentlagerId_soknad_2 = "1234"

private const val dokumenttype = "type"
private const val dokumenttype_2 = "type 2"
private const val dokumenttype_3 = "type 3"
private const val dokumenttype_4 = "type 4"

private val tid_1 = Instant.now()
private val tid_2 = Instant.now().minus(2, ChronoUnit.DAYS)
private val tid_soknad = Instant.now().minus(14, ChronoUnit.DAYS)

private const val vedleggMetadata_ettersendelse_1 = "vedlegg metadata 1"
private const val vedleggMetadata_ettersendelse_2 = "vedlegg metadata 2"
private const val vedleggMetadata_ettersendelse_3 = "vedlegg metadata 3"
private const val vedleggMetadata_ettersendelse_4 = "vedlegg metadata 4"
private const val vedleggMetadata_ettersendelse_5 = "vedlegg metadata 5"
private const val vedleggMetadata_soknad = "vedlegg metadata soknad"

private val ettersendelser = listOf(
        Ettersendelse(
                navEksternRefId = "ref 1",
                vedleggMetadata = vedleggMetadata_ettersendelse_1,
                vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 42), DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_2, 42)),
                timestampSendt = tid_1.toEpochMilli()),
        Ettersendelse(
                navEksternRefId = "ref 2",
                vedleggMetadata = vedleggMetadata_ettersendelse_2,
                vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_3, dokumentlagerId_3, 42), DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_4, 84)),
                timestampSendt = tid_2.toEpochMilli()),
        Ettersendelse(
                navEksternRefId = "ref 2",
                vedleggMetadata = vedleggMetadata_ettersendelse_4,
                vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_3, 1), DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_4, 2)),
                timestampSendt = tid_2.toEpochMilli())
)

private val originalSoknad = OriginalSoknadNAV(
        navEksternRefId = "123",
        metadata = "metadata",
        vedleggMetadata = vedleggMetadata_soknad,
        soknadDokument = mockk(),
        vedlegg = listOf(DokumentInfo(soknad_filnavn_1, dokumentlagerId_soknad_1, 1337), DokumentInfo(soknad_filnavn_2, dokumentlagerId_soknad_2, 1337)),
        timestampSendt = tid_soknad.toEpochMilli()
)

private val soknadVedleggSpesifikasjon = JsonVedleggSpesifikasjon()
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
                        .withType(dokumenttype_2)
        ))

private val ettersendteVedleggSpesifikasjon_1 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_1).withSha512("g25b3")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_3),
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("4avc65a8")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_4)
        ))

private val ettersendteVedleggSpesifikasjon_2 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_3).withSha512("aadsfwr"),
                                JsonFiler().withFilnavn(ettersendelse_filnavn_4).withSha512("uiuusss")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_3)
        ))

private val ettersendteVedleggSpesifikasjon_3 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_3).withSha512("aadsfwr")))
                        .withStatus("VedleggAlleredeSendt")
                        .withType(dokumenttype_3)
        ))

private val ettersendteVedleggSpesifikasjon_4 = JsonVedleggSpesifikasjon()
        .withVedlegg(listOf(
                JsonVedlegg()
                        .withFiler(listOf(
                                JsonFiler().withFilnavn(ettersendelse_filnavn_4).withSha512("1231231")))
                        .withStatus("LastetOpp")
                        .withType(dokumenttype_3)
        ))