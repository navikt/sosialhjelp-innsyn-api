package no.nav.sosialhjelp.innsyn.service.vedlegg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.Ettersendelse
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal class VedleggServiceTest {

    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()

    private val service = VedleggService(fiksClient)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonVedleggSpesifikasjon: JsonVedleggSpesifikasjon = mockk()
    private val model = InternalDigisosSoker()

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV } returns originalSoknad
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns ettersendelser
        every { mockDigisosSak.fiksDigisosId } returns "fiksDigisosId"

        every { mockJsonVedleggSpesifikasjon.vedlegg } returns emptyList()

        every { fiksClient.hentDokument(any(), vedleggMetadata_soknad_1, any(), "token") } returns soknadVedleggSpesifikasjon
        every { fiksClient.hentDokument(any(), vedleggMetadata_soknad_2, any(), "token") } returns soknadVedleggSpesifikasjonMedStatusKrevesOgLastetOpp
        every { fiksClient.hentDokument(any(), vedleggMetadata_ettersendelse_1, any(), "token") } returns ettersendteVedleggSpesifikasjon_1
        every { fiksClient.hentDokument(any(), vedleggMetadata_ettersendelse_2, any(), "token") } returns ettersendteVedleggSpesifikasjon_2
        every { fiksClient.hentDokument(any(), vedleggMetadata_ettersendelse_3, any(), "token") } returns ettersendteVedleggSpesifikasjon_3
        every { fiksClient.hentDokument(any(), vedleggMetadata_ettersendelse_4, any(), "token") } returns ettersendteVedleggSpesifikasjon_4
        every { fiksClient.hentDokument(any(), vedleggMetadata_ettersendelse_5, any(), "token") } returns ettersendteVedleggSpesifikasjon_5
    }

    @Test
    fun `skal returnere emptylist hvis soknad har null vedlegg og ingen ettersendelser finnes`() {
        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentDokument(any(), vedleggMetadata_soknad_1, any(), any()) } returns mockJsonVedleggSpesifikasjon
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model, "token")
        assertThat(list).isEmpty()
    }

    @Test
    fun `skal kun returnere soknadens vedlegg hvis ingen ettersendelser finnes`() {
        every { eventService.createModel(any(), any()) } returns model
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model, "token")

        assertThat(list).hasSize(2)
        assertThat(list[0].type).isEqualTo(dokumenttype)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(soknad_filnavn_1)
        assertThat(list[1].type).isEqualTo(dokumenttype_2)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(soknad_filnavn_2)
    }

    @Test
    fun `skal filtrere vekk vedlegg som ikke er LastetOpp`() {
        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentDokument(any(), vedleggMetadata_soknad_1, any(), any()) } returns mockJsonVedleggSpesifikasjon
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns listOf(
            Ettersendelse(
                navEksternRefId = "ref 3",
                vedleggMetadata = vedleggMetadata_ettersendelse_3,
                vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 42)),
                timestampSendt = tid_1.toEpochMilli()
            )
        )

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model, "token")

        assertThat(list).hasSize(0)
    }

    @Test
    fun `skal kun returne ettersendte vedlegg hvis soknaden ikke har noen vedlegg`() {
        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentDokument(any(), vedleggMetadata_soknad_1, any(), any()) } returns mockJsonVedleggSpesifikasjon

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model, "token")

        assertThat(list).hasSize(4)
        assertThat(list[0].type).isEqualTo(dokumenttype_3)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_1)

        assertThat(list[1].type).isEqualTo(dokumenttype_4)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_2)

        assertThat(list[2].type).isEqualTo(dokumenttype_3)
        assertThat(list[2].dokumentInfoList).hasSize(3)
        assertThat(list[2].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_3)
        assertThat(list[2].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_4)
        assertThat(list[2].dokumentInfoList[2].filnavn).isEqualTo(ettersendelse_filnavn_4)

        assertThat(list[3].type).isEqualTo(dokumenttype)
        assertThat(list[3].dokumentInfoList).hasSize(2)
        assertThat(list[3].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_1)
        assertThat(list[3].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_5)
    }

    @Test
    fun `skal hente alle vedlegg for digisosSak`() {
        every { eventService.createModel(any(), any()) } returns model

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model, "token")

        assertThat(list).hasSize(6)

        // nano-presisjon lacking
        val zoneIdOslo = ZoneId.of("Europe/Oslo")
        assertThat(list[0].type).isEqualTo(dokumenttype)
        assertThat(list[0].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))

        assertThat(list[1].type).isEqualTo(dokumenttype_2)
        assertThat(list[1].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))

        assertThat(list[2].type).isEqualTo(dokumenttype_3)
        assertThat(list[2].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_1, zoneIdOslo))

        assertThat(list[3].type).isEqualTo(dokumenttype_4)
        assertThat(list[3].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_1, zoneIdOslo))

        assertThat(list[4].type).isEqualTo(dokumenttype_3)
        assertThat(list[4].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_2, zoneIdOslo))

        assertThat(list[5].type).isEqualTo(dokumenttype)
        assertThat(list[5].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_1, zoneIdOslo))
    }

    @Test
    fun `skal hente soknadsvedlegg filtrert pa status for digisosSak`() {
        every { mockDigisosSak.originalSoknadNAV } returns originalSoknadMedVedleggKrevesOgLastetOpp
        val lastetOppList = service.hentSoknadVedleggMedStatus(LASTET_OPP_STATUS, mockDigisosSak, "token")
        val vedleggKrevesList = service.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, mockDigisosSak, "token")

        assertThat(lastetOppList).hasSize(1)
        assertThat(vedleggKrevesList).hasSize(1)

        // nano-presisjon lacking
        val zoneIdOslo = ZoneId.of("Europe/Oslo")
        assertThat(lastetOppList[0].type).isEqualTo(dokumenttype)
        assertThat(lastetOppList[0].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))

        assertThat(vedleggKrevesList[0].type).isEqualTo(dokumenttype_2)
        assertThat(vedleggKrevesList[0].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))
    }

    @Test
    fun `like filnavn i DokumentInfoList vil resultere i at de returneres for hver JsonFil med riktig filnavn`() {
        val model = InternalDigisosSoker()

        every { eventService.createModel(any(), any()) } returns model
        every { fiksClient.hentDokument(any(), vedleggMetadata_soknad_1, any(), any()) } returns mockJsonVedleggSpesifikasjon
        every { fiksClient.hentDokument(any(), vedleggMetadata_ettersendelse_5, any(), any()) } returns
            JsonVedleggSpesifikasjon()
                .withVedlegg(
                    listOf(
                        JsonVedlegg()
                            .withFiler(
                                listOf(
                                    JsonFiler().withFilnavn(ettersendelse_filnavn_1).withSha512("1231231"),
                                    JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("adfgbjn")
                                )
                            )
                            .withStatus(LASTET_OPP_STATUS)
                            .withType(dokumenttype_3),
                        JsonVedlegg()
                            .withFiler(
                                listOf(
                                    JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("aasdcx"),
                                    JsonFiler().withFilnavn(ettersendelse_filnavn_4).withSha512("qweqqa")
                                )
                            )
                            .withStatus(LASTET_OPP_STATUS)
                            .withType(dokumenttype_4)
                    )
                )

        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns listOf(
            Ettersendelse(
                navEksternRefId = "ref 3",
                vedleggMetadata = vedleggMetadata_ettersendelse_5,
                vedlegg = listOf(
                    DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 1),
                    DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_2, 2), // samme filnavn
                    DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_3, 3), // samme filnavn
                    DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_4, 4)
                ),
                timestampSendt = tid_1.toEpochMilli()
            )
        )

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model, "token")

        assertThat(list).hasSize(2)

        assertThat(list[0].dokumentInfoList).hasSize(2)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_1)
        assertThat(list[0].dokumentInfoList[0].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_1)
        assertThat(list[0].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[0].dokumentInfoList[1].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_2)

        assertThat(list[1].dokumentInfoList).hasSize(2)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(ettersendelse_filnavn_2)
        assertThat(list[1].dokumentInfoList[0].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_3)
        assertThat(list[1].dokumentInfoList[1].filnavn).isEqualTo(ettersendelse_filnavn_4)
        assertThat(list[1].dokumentInfoList[1].dokumentlagerDokumentId).isEqualTo(dokumentlagerId_4)
    }
}

private const val ettersendelse_filnavn_1 = "filnavn.pdf"
private const val ettersendelse_filnavn_2 = "navn på fil.ocr"
private const val ettersendelse_filnavn_3 = "denne filens navn.jpg"
private const val ettersendelse_filnavn_4 = "gif.jpg"
private const val ettersendelse_filnavn_5 = "ikke gif.jpg"
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
private const val vedleggMetadata_soknad_1 = "vedlegg metadata soknad"
private const val vedleggMetadata_soknad_2 = "vedlegg metadata soknad med vedlegg kreves og lastet opp"

private val ettersendelser = listOf(
    Ettersendelse(
        navEksternRefId = "ref 1",
        vedleggMetadata = vedleggMetadata_ettersendelse_1,
        vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_1, 42), DokumentInfo(ettersendelse_filnavn_2, dokumentlagerId_2, 42)),
        timestampSendt = tid_1.toEpochMilli()
    ),
    Ettersendelse(
        navEksternRefId = "ref 2",
        vedleggMetadata = vedleggMetadata_ettersendelse_2,
        vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_3, dokumentlagerId_3, 42), DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_4, 84)),
        timestampSendt = tid_2.toEpochMilli()
    ),
    Ettersendelse(
        navEksternRefId = "ref 2",
        vedleggMetadata = vedleggMetadata_ettersendelse_4,
        vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_3, 1), DokumentInfo(ettersendelse_filnavn_4, dokumentlagerId_4, 2)),
        timestampSendt = tid_2.toEpochMilli()
    ),
    Ettersendelse(
        navEksternRefId = "ref 3",
        vedleggMetadata = vedleggMetadata_ettersendelse_5,
        vedlegg = listOf(DokumentInfo(ettersendelse_filnavn_1, dokumentlagerId_2, 1), DokumentInfo(ettersendelse_filnavn_5, dokumentlagerId_1, 2)),
        timestampSendt = tid_1.toEpochMilli()
    )
)

private val originalSoknad = OriginalSoknadNAV(
    navEksternRefId = "123",
    metadata = "metadata",
    vedleggMetadata = vedleggMetadata_soknad_1,
    soknadDokument = mockk(),
    vedlegg = listOf(DokumentInfo(soknad_filnavn_1, dokumentlagerId_soknad_1, 1337), DokumentInfo(soknad_filnavn_2, dokumentlagerId_soknad_2, 1337)),
    timestampSendt = tid_soknad.toEpochMilli()
)

private val originalSoknadMedVedleggKrevesOgLastetOpp = OriginalSoknadNAV(
    navEksternRefId = "123",
    metadata = "metadata",
    vedleggMetadata = vedleggMetadata_soknad_2,
    soknadDokument = mockk(),
    vedlegg = listOf(DokumentInfo(soknad_filnavn_1, dokumentlagerId_soknad_1, 1337)),
    timestampSendt = tid_soknad.toEpochMilli()
)

private val soknadVedleggSpesifikasjon = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(soknad_filnavn_1).withSha512("1234fasd")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype),
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(soknad_filnavn_2).withSha512("sfg234")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype_2)
        )
    )

private val soknadVedleggSpesifikasjonMedStatusKrevesOgLastetOpp = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(soknad_filnavn_1).withSha512("1234fasd")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype),
            JsonVedlegg()
                .withFiler(listOf())
                .withStatus("VedleggKreves")
                .withType(dokumenttype_2)
        )
    )

private val ettersendteVedleggSpesifikasjon_1 = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(ettersendelse_filnavn_1).withSha512("g25b3")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype_3),
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(ettersendelse_filnavn_2).withSha512("4avc65a8")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype_4)
        )
    )

private val ettersendteVedleggSpesifikasjon_2 = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(ettersendelse_filnavn_3).withSha512("aadsfwr"),
                        JsonFiler().withFilnavn(ettersendelse_filnavn_4).withSha512("uiuusss")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype_3)
        )
    )

private val ettersendteVedleggSpesifikasjon_3 = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(ettersendelse_filnavn_3).withSha512("aadsfwr")
                    )
                )
                .withStatus("VedleggAlleredeSendt")
                .withType(dokumenttype_3)
        )
    )

private val ettersendteVedleggSpesifikasjon_4 = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(ettersendelse_filnavn_4).withSha512("1231231")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype_3)
        )
    )

private val ettersendteVedleggSpesifikasjon_5 = JsonVedleggSpesifikasjon()
    .withVedlegg(
        listOf(
            JsonVedlegg()
                .withFiler(
                    listOf(
                        JsonFiler().withFilnavn(ettersendelse_filnavn_1).withSha512("1231231"),
                        JsonFiler().withFilnavn(ettersendelse_filnavn_5).withSha512("9786468")
                    )
                )
                .withStatus(LASTET_OPP_STATUS)
                .withType(dokumenttype)
        )
    )
