package no.nav.sosialhjelp.innsyn.vedlegg

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonFiler
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.api.fiks.Ettersendelse
import no.nav.sosialhjelp.api.fiks.OriginalSoknadNAV
import no.nav.sosialhjelp.innsyn.digisosapi.FiksService
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
    private val fiksService: FiksService = mockk()

    private val service = VedleggService(fiksService)

    private val mockDigisosSak: DigisosSak = mockk()
    private val mockJsonVedleggSpesifikasjon: JsonVedleggSpesifikasjon = mockk()
    private val model = InternalDigisosSoker()

    @BeforeEach
    internal fun setUp() {
        clearAllMocks()

        coEvery { fiksService.getSoknad(any()) } returns mockDigisosSak
        every { mockDigisosSak.originalSoknadNAV } returns originalSoknad
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns ettersendelser
        every { mockDigisosSak.fiksDigisosId } returns "fiksDigisosId"

        every { mockJsonVedleggSpesifikasjon.vedlegg } returns emptyList()

        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_SOKNAD_1, any())
        } returns soknadVedleggSpesifikasjon
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_SOKNAD_2, any())
        } returns soknadVedleggSpesifikasjonMedStatusKrevesOgLastetOpp
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_ETTERSENDELSE_1, any())
        } returns ettersendteVedleggSpesifikasjon_1
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_ETTERSENDELSE_2, any())
        } returns ettersendteVedleggSpesifikasjon_2
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_ETTERSENDELSE_3, any())
        } returns ettersendteVedleggSpesifikasjon_3
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_ETTERSENDELSE_4, any())
        } returns ettersendteVedleggSpesifikasjon_4
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_ETTERSENDELSE_5, any())
        } returns ettersendteVedleggSpesifikasjon_5
    }

    @Test
    suspend fun `skal returnere emptylist hvis soknad har null vedlegg og ingen ettersendelser finnes`() {
        coEvery { eventService.createModel(any()) } returns model
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_SOKNAD_1, any())
        } returns mockJsonVedleggSpesifikasjon
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model)
        assertThat(list).isEmpty()
    }

    @Test
    suspend fun `skal kun returnere soknadens vedlegg hvis ingen ettersendelser finnes`() {
        coEvery { eventService.createModel(any()) } returns model
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns emptyList()

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model)

        assertThat(list).hasSize(2)
        assertThat(list[0].type).isEqualTo(DOKUMENTTYPE)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(SOKNAD_FILNAVN_1)
        assertThat(list[1].type).isEqualTo(DOKUMENTTYPE_2)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(SOKNAD_FILNAVN_2)
    }

    @Test
    suspend fun `skal filtrere vekk vedlegg som ikke er LastetOpp`() {
        coEvery { eventService.createModel(any()) } returns model
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_SOKNAD_1, any())
        } returns mockJsonVedleggSpesifikasjon
        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns
            listOf(
                Ettersendelse(
                    navEksternRefId = "ref 3",
                    vedleggMetadata = VEDLEGG_METADATA_ETTERSENDELSE_3,
                    vedlegg = listOf(DokumentInfo(ETTERSENDELSE_FILNAVN_1, DOKUMENTLAGERID_1, 42)),
                    timestampSendt = tid_1.toEpochMilli(),
                ),
            )

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model)

        assertThat(list).hasSize(0)
    }

    @Test
    suspend fun `skal kun returne ettersendte vedlegg hvis soknaden ikke har noen vedlegg`() {
        coEvery { eventService.createModel(any()) } returns model
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_SOKNAD_1, any())
        } returns mockJsonVedleggSpesifikasjon

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model)

        assertThat(list).hasSize(4)
        assertThat(list[0].type).isEqualTo(DOKUMENTTYPE_3)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_1)

        assertThat(list[1].type).isEqualTo(DOKUMENTTYPE_4)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_2)

        assertThat(list[2].type).isEqualTo(DOKUMENTTYPE_3)
        assertThat(list[2].dokumentInfoList).hasSize(3)
        assertThat(list[2].dokumentInfoList[0].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_3)
        assertThat(list[2].dokumentInfoList[1].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_4)
        assertThat(list[2].dokumentInfoList[2].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_4)

        assertThat(list[3].type).isEqualTo(DOKUMENTTYPE)
        assertThat(list[3].dokumentInfoList).hasSize(2)
        assertThat(list[3].dokumentInfoList[0].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_1)
        assertThat(list[3].dokumentInfoList[1].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_5)
    }

    @Test
    suspend fun `skal hente alle vedlegg for digisosSak`() {
        coEvery { eventService.createModel(any()) } returns model

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model)

        assertThat(list).hasSize(6)

        // nano-presisjon lacking
        val zoneIdOslo = ZoneId.of("Europe/Oslo")
        assertThat(list[0].type).isEqualTo(DOKUMENTTYPE)
        assertThat(list[0].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))

        assertThat(list[1].type).isEqualTo(DOKUMENTTYPE_2)
        assertThat(list[1].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))

        assertThat(list[2].type).isEqualTo(DOKUMENTTYPE_3)
        assertThat(list[2].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_1, zoneIdOslo))

        assertThat(list[3].type).isEqualTo(DOKUMENTTYPE_4)
        assertThat(list[3].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_1, zoneIdOslo))

        assertThat(list[4].type).isEqualTo(DOKUMENTTYPE_3)
        assertThat(list[4].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_2, zoneIdOslo))

        assertThat(list[5].type).isEqualTo(DOKUMENTTYPE)
        assertThat(list[5].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_1, zoneIdOslo))
    }

    @Test
    suspend fun `skal hente soknadsvedlegg filtrert pa status for digisosSak`() {
        every { mockDigisosSak.originalSoknadNAV } returns originalSoknadMedVedleggKrevesOgLastetOpp
        val lastetOppList = service.hentSoknadVedleggMedStatus(LASTET_OPP_STATUS, mockDigisosSak)
        val vedleggKrevesList = service.hentSoknadVedleggMedStatus(VEDLEGG_KREVES_STATUS, mockDigisosSak)

        assertThat(lastetOppList).hasSize(1)
        assertThat(vedleggKrevesList).hasSize(1)

        // nano-presisjon lacking
        val zoneIdOslo = ZoneId.of("Europe/Oslo")
        assertThat(lastetOppList[0].type).isEqualTo(DOKUMENTTYPE)
        assertThat(lastetOppList[0].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))

        assertThat(vedleggKrevesList[0].type).isEqualTo(DOKUMENTTYPE_2)
        assertThat(vedleggKrevesList[0].tidspunktLastetOpp).isEqualToIgnoringNanos(LocalDateTime.ofInstant(tid_soknad, zoneIdOslo))
    }

    @Test
    suspend fun `Verifisere at antall JsonVedlegg ender i riktig antall internalVedlegg og at de opprinnelige filene finnes`() {
        val model = InternalDigisosSoker()

        coEvery { eventService.createModel(any()) } returns model
        coEvery {
            fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_SOKNAD_1, any())
        } returns mockJsonVedleggSpesifikasjon
        coEvery { fiksService.getDocument<JsonVedleggSpesifikasjon>(any(), VEDLEGG_METADATA_ETTERSENDELSE_5, any()) } returns
            JsonVedleggSpesifikasjon(
                vedlegg =
                    listOf(
                        JsonVedlegg(
                            type = DOKUMENTTYPE_3,
                            status = LASTET_OPP_STATUS,
                            filer = listOf(JsonFiler(ETTERSENDELSE_FILNAVN_1, "1231231"), JsonFiler(ETTERSENDELSE_FILNAVN_2, "adfgbjn")),
                        ),
                        JsonVedlegg(
                            type = DOKUMENTTYPE_4,
                            status = LASTET_OPP_STATUS,
                            filer = listOf(JsonFiler(ETTERSENDELSE_FILNAVN_3, "aasdcx"), JsonFiler(ETTERSENDELSE_FILNAVN_4, "qweqqa")),
                        ),
                    ),
            )

        every { mockDigisosSak.ettersendtInfoNAV?.ettersendelser } returns
            listOf(
                Ettersendelse(
                    navEksternRefId = "ref 3",
                    vedleggMetadata = VEDLEGG_METADATA_ETTERSENDELSE_5,
                    vedlegg =
                        listOf(
                            DokumentInfo(ETTERSENDELSE_FILNAVN_1, DOKUMENTLAGERID_1, 1),
                            // samme filnavn
                            DokumentInfo(ETTERSENDELSE_FILNAVN_2, DOKUMENTLAGERID_2, 2),
                            // samme filnavn
                            DokumentInfo(ETTERSENDELSE_FILNAVN_3, DOKUMENTLAGERID_3, 3),
                            DokumentInfo(ETTERSENDELSE_FILNAVN_4, DOKUMENTLAGERID_4, 4),
                        ),
                    timestampSendt = tid_1.toEpochMilli(),
                ),
            )

        val list = service.hentAlleOpplastedeVedlegg(mockDigisosSak, model)

        assertThat(list).hasSize(2)

        assertThat(list[0].dokumentInfoList).hasSize(2)
        assertThat(list[0].dokumentInfoList[0].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_1)
        assertThat(list[0].dokumentInfoList[0].dokumentlagerDokumentId).isEqualTo(DOKUMENTLAGERID_1)
        assertThat(list[0].dokumentInfoList[1].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_2)
        assertThat(list[0].dokumentInfoList[1].dokumentlagerDokumentId).isEqualTo(DOKUMENTLAGERID_2)

        assertThat(list[1].dokumentInfoList).hasSize(2)
        assertThat(list[1].dokumentInfoList[0].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_3)
        assertThat(list[1].dokumentInfoList[0].dokumentlagerDokumentId).isEqualTo(DOKUMENTLAGERID_3)
        assertThat(list[1].dokumentInfoList[1].filnavn).isEqualTo(ETTERSENDELSE_FILNAVN_4)
        assertThat(list[1].dokumentInfoList[1].dokumentlagerDokumentId).isEqualTo(DOKUMENTLAGERID_4)
    }
}

// filnavn lastes alltid opp med en del av UUID lagt til filnavnet - så dette er ikke reelle filnavn
private const val ETTERSENDELSE_FILNAVN_1 = "filnavn.pdf"
private const val ETTERSENDELSE_FILNAVN_2 = "navn på fil.ocr"
private const val ETTERSENDELSE_FILNAVN_3 = "denne filens navn.jpg"
private const val ETTERSENDELSE_FILNAVN_4 = "gif.jpg"
private const val ETTERSENDELSE_FILNAVN_5 = "ikke gif.jpg"
private const val SOKNAD_FILNAVN_1 = "originalSoknadVedlegg.png"
private const val SOKNAD_FILNAVN_2 = "originalSoknadVedlegg_2.exe"

private const val DOKUMENTLAGERID_1 = "9999"
private const val DOKUMENTLAGERID_2 = "7777"
private const val DOKUMENTLAGERID_3 = "5555"
private const val DOKUMENTLAGERID_4 = "3333"
private const val DOKUMENTLAGERID_SOKNAD_1 = "1111"
private const val DOKUMENTLAGERID_SOKNAD_2 = "1234"

private const val DOKUMENTTYPE = "type"
private const val DOKUMENTTYPE_2 = "type 2"
private const val DOKUMENTTYPE_3 = "type 3"
private const val DOKUMENTTYPE_4 = "type 4"

private val tid_1 = Instant.now()
private val tid_2 = Instant.now().minus(2, ChronoUnit.DAYS)
private val tid_soknad = Instant.now().minus(14, ChronoUnit.DAYS)

private const val VEDLEGG_METADATA_ETTERSENDELSE_1 = "vedlegg metadata 1"
private const val VEDLEGG_METADATA_ETTERSENDELSE_2 = "vedlegg metadata 2"
private const val VEDLEGG_METADATA_ETTERSENDELSE_3 = "vedlegg metadata 3"
private const val VEDLEGG_METADATA_ETTERSENDELSE_4 = "vedlegg metadata 4"
private const val VEDLEGG_METADATA_ETTERSENDELSE_5 = "vedlegg metadata 5"
private const val VEDLEGG_METADATA_SOKNAD_1 = "vedlegg metadata soknad"
private const val VEDLEGG_METADATA_SOKNAD_2 = "vedlegg metadata soknad med vedlegg kreves og lastet opp"

private val ettersendelser =
    listOf(
        Ettersendelse(
            navEksternRefId = "ref 1",
            vedleggMetadata = VEDLEGG_METADATA_ETTERSENDELSE_1,
            vedlegg =
                listOf(
                    DokumentInfo(ETTERSENDELSE_FILNAVN_1, DOKUMENTLAGERID_1, 42),
                    DokumentInfo(ETTERSENDELSE_FILNAVN_2, DOKUMENTLAGERID_2, 42),
                ),
            timestampSendt = tid_1.toEpochMilli(),
        ),
        Ettersendelse(
            navEksternRefId = "ref 2",
            vedleggMetadata = VEDLEGG_METADATA_ETTERSENDELSE_2,
            vedlegg =
                listOf(
                    DokumentInfo(ETTERSENDELSE_FILNAVN_3, DOKUMENTLAGERID_3, 42),
                    DokumentInfo(ETTERSENDELSE_FILNAVN_4, DOKUMENTLAGERID_4, 84),
                ),
            timestampSendt = tid_2.toEpochMilli(),
        ),
        Ettersendelse(
            navEksternRefId = "ref 2",
            vedleggMetadata = VEDLEGG_METADATA_ETTERSENDELSE_4,
            vedlegg =
                listOf(
                    DokumentInfo(ETTERSENDELSE_FILNAVN_4, DOKUMENTLAGERID_3, 1),
                    DokumentInfo(ETTERSENDELSE_FILNAVN_4, DOKUMENTLAGERID_4, 2),
                ),
            timestampSendt = tid_2.toEpochMilli(),
        ),
        Ettersendelse(
            navEksternRefId = "ref 3",
            vedleggMetadata = VEDLEGG_METADATA_ETTERSENDELSE_5,
            vedlegg =
                listOf(
                    DokumentInfo(ETTERSENDELSE_FILNAVN_1, DOKUMENTLAGERID_2, 1),
                    DokumentInfo(ETTERSENDELSE_FILNAVN_5, DOKUMENTLAGERID_1, 2),
                ),
            timestampSendt = tid_1.toEpochMilli(),
        ),
    )

private val originalSoknad =
    OriginalSoknadNAV(
        navEksternRefId = "123",
        metadata = "metadata",
        vedleggMetadata = VEDLEGG_METADATA_SOKNAD_1,
        soknadDokument = mockk(),
        vedlegg =
            listOf(
                DokumentInfo(SOKNAD_FILNAVN_1, DOKUMENTLAGERID_SOKNAD_1, 1337),
                DokumentInfo(SOKNAD_FILNAVN_2, DOKUMENTLAGERID_SOKNAD_2, 1337),
            ),
        timestampSendt = tid_soknad.toEpochMilli(),
    )

private val originalSoknadMedVedleggKrevesOgLastetOpp =
    OriginalSoknadNAV(
        navEksternRefId = "123",
        metadata = "metadata",
        vedleggMetadata = VEDLEGG_METADATA_SOKNAD_2,
        soknadDokument = mockk(),
        vedlegg = listOf(DokumentInfo(SOKNAD_FILNAVN_1, DOKUMENTLAGERID_SOKNAD_1, 1337)),
        timestampSendt = tid_soknad.toEpochMilli(),
    )

private val soknadVedleggSpesifikasjon =
    JsonVedleggSpesifikasjon(
        listOf(
            vedlegg(DOKUMENTTYPE, LASTET_OPP_STATUS, JsonFiler(SOKNAD_FILNAVN_1, "1234fasd")),
            vedlegg(DOKUMENTTYPE_2, LASTET_OPP_STATUS, JsonFiler(SOKNAD_FILNAVN_2, "sfg234")),
        ),
    )

private val soknadVedleggSpesifikasjonMedStatusKrevesOgLastetOpp =
    JsonVedleggSpesifikasjon(
        listOf(vedlegg(DOKUMENTTYPE, LASTET_OPP_STATUS, JsonFiler(SOKNAD_FILNAVN_1, "1234fasd")), vedlegg(DOKUMENTTYPE_2, "VedleggKreves")),
    )

private val ettersendteVedleggSpesifikasjon_1 =
    JsonVedleggSpesifikasjon(
        listOf(
            vedlegg(DOKUMENTTYPE_3, LASTET_OPP_STATUS, JsonFiler(ETTERSENDELSE_FILNAVN_1, "g25b3")),
            vedlegg(DOKUMENTTYPE_4, LASTET_OPP_STATUS, JsonFiler(ETTERSENDELSE_FILNAVN_2, "4avc65a8")),
        ),
    )

private val ettersendteVedleggSpesifikasjon_2 =
    JsonVedleggSpesifikasjon(
        listOf(
            vedlegg(
                DOKUMENTTYPE_3,
                LASTET_OPP_STATUS,
                JsonFiler(ETTERSENDELSE_FILNAVN_3, "aadsfwr"),
                JsonFiler(ETTERSENDELSE_FILNAVN_4, "uiuusss"),
            ),
        ),
    )

private val ettersendteVedleggSpesifikasjon_3 =
    JsonVedleggSpesifikasjon(listOf(vedlegg(DOKUMENTTYPE_3, "VedleggAlleredeSendt", JsonFiler(ETTERSENDELSE_FILNAVN_3, "aadsfwr"))))

private val ettersendteVedleggSpesifikasjon_4 =
    JsonVedleggSpesifikasjon(listOf(vedlegg(DOKUMENTTYPE_3, LASTET_OPP_STATUS, JsonFiler(ETTERSENDELSE_FILNAVN_4, "1231231"))))

private val ettersendteVedleggSpesifikasjon_5 =
    JsonVedleggSpesifikasjon(
        listOf(
            vedlegg(
                DOKUMENTTYPE,
                LASTET_OPP_STATUS,
                JsonFiler(ETTERSENDELSE_FILNAVN_1, "1231231"),
                JsonFiler(ETTERSENDELSE_FILNAVN_5, "9786468"),
            ),
        ),
    )

private fun vedlegg(
    type: String,
    status: String,
    vararg filer: JsonFiler,
) = JsonVedlegg(type = type, status = status, filer = filer.toList())
