package no.nav.sosialhjelp.innsyn.service.vedlegg

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.finn.unleash.Unleash
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.client.virusscan.VirusScanner
import no.nav.sosialhjelp.innsyn.common.OpplastingFilnavnMismatchException
import no.nav.sosialhjelp.innsyn.common.VirusScanException
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.rest.OpplastetFil
import no.nav.sosialhjelp.innsyn.rest.OpplastetVedleggMetadata
import no.nav.sosialhjelp.innsyn.service.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.service.vedlegg.VedleggOpplastingService.Companion.containsIllegalCharacters
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.util.UUID
import javax.imageio.ImageIO

internal class VedleggOpplastingServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val virusScanner: VirusScanner = mockk()
    private val redisService: RedisService = mockk()
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()
    private val unleashClient: Unleash = mockk()
    private val service = VedleggOpplastingService(
        fiksClient,
        krypteringService,
        virusScanner,
        redisService,
        ettersendelsePdfGenerator,
        dokumentlagerClient,
        unleashClient
    )

    private val mockDigisosSak: DigisosSak = mockk(relaxed = true)

    private val id = "123"

    private val type0 = "brukskonto"
    private val tilleggsinfo0 = "kontoutskrift"
    private val filnavn0 = "test0.jpg"
    private val filtype0 = "image/png"
    private val type1 = "bsu"
    private val tilleggsinfo1 = "kontoutskrift"
    private val filnavn1 = "test1.png"
    private val filtype1 = "image/jpeg"
    private val filnavn2 = "test2.lol"

    private val pngFile = createImageByteArray("png")
    private val jpgFile = createImageByteArray("jpg")

    private val mockCertificate: X509Certificate = mockk()

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.fiksDigisosId } returns id
        every { virusScanner.scan(any(), any()) } just runs
        every { redisService.put(any(), any(), any()) } just runs
        every { redisService.defaultTimeToLiveSeconds } returns 1
        every { dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate() } returns mockCertificate
        every { unleashClient.isEnabled(any(), false) } returns true
    }

    @Test
    fun `sendVedleggTilFiks skal kalle FiksClient med gyldige filer for opplasting`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val ettersendelsPdf = ByteArray(1)
        every { ettersendelsePdfGenerator.generate(any(), any()) } returns ettersendelsPdf

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "uuid"

        val largePngFile = createImageByteArray("png", 2)
        val filnavn3 = "test3.png"

        val filnavn4 = " jadda.png"
        val filnavn5 = " uten "

        val metadata = mutableListOf(
            OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1), OpplastetFil(filnavn1)), null),
            OpplastetVedleggMetadata(type1, tilleggsinfo1, null, null, mutableListOf(OpplastetFil(filnavn3), OpplastetFil(filnavn4), OpplastetFil(filnavn5)), null)
        )
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn0, filtype1, jpgFile),
            MockMultipartFile("files", filnavn1, filtype0, pngFile),
            MockMultipartFile("files", filnavn1, filtype0, largePngFile),
            MockMultipartFile("files", filnavn3, filtype0, pngFile),
            MockMultipartFile("files", filnavn4, filtype0, pngFile),
            MockMultipartFile("files", filnavn5, filtype0, pngFile)
        )

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        val filerForOpplastingSlot = slot<List<FilForOpplasting>>()
        val vedleggSpesifikasjonSlot = slot<JsonVedleggSpesifikasjon>()
        verify(exactly = 1) { fiksClient.lastOppNyEttersendelse(capture(filerForOpplastingSlot), capture(vedleggSpesifikasjonSlot), any(), any()) }
        val filerForOpplasting = filerForOpplastingSlot.captured
        val vedleggSpesifikasjon = vedleggSpesifikasjonSlot.captured

        assertThat(filerForOpplasting).hasSize(7) // Inkluderer ettersendelse.pdf
        assertThat(filerForOpplasting[0].filnavn == filnavn0)
        assertThat(filerForOpplasting[0].mimetype == filtype0)
        assertThat(filerForOpplasting[1].filnavn == filnavn1)
        assertThat(filerForOpplasting[1].mimetype == filtype1)
        assertThat(filerForOpplasting[2].filnavn == filnavn1)
        assertThat(filerForOpplasting[2].mimetype == filtype1)
        assertThat(filerForOpplasting[3].filnavn == filnavn1)
        assertThat(filerForOpplasting[3].mimetype == filtype1)

        assertThat(vedleggSpesifikasjon.vedlegg).hasSize(2)
        assertThat(vedleggSpesifikasjon.vedlegg[0].type == type0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].tilleggsinfo == tilleggsinfo0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].status == "LastetOpp")
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer).hasSize(3)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[0].filnavn == filnavn0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[1].filnavn == filnavn1)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[2].filnavn == filnavn1)

        assertThat(vedleggSpesifikasjon.vedlegg[1].type).isEqualTo(type1)
        assertThat(vedleggSpesifikasjon.vedlegg[1].tilleggsinfo).isEqualTo(tilleggsinfo1)
        assertThat(vedleggSpesifikasjon.vedlegg[1].status).isEqualTo("LastetOpp")
        assertThat(vedleggSpesifikasjon.vedlegg[1].filer).hasSize(3)
        assertThat(vedleggSpesifikasjon.vedlegg[1].filer[0].filnavn.replace("-uuid", "")).isEqualTo(filnavn3)
        assertThat(vedleggSpesifikasjon.vedlegg[1].filer[1].filnavn.replace("-uuid", "")).isEqualTo(filnavn4.trim())
        assertThat(vedleggSpesifikasjon.vedlegg[1].filer[2].filnavn.replace("-uuid", "")).startsWith(filnavn5.trim()).endsWith(".png")

        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[1].sha512).isNotEqualTo(vedleggSpesifikasjon.vedlegg[0].filer[2].sha512)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[1].sha512).isEqualTo(vedleggSpesifikasjon.vedlegg[1].filer[0].sha512)

        assertThat(vedleggOpplastingResponseList[0].filer[0].filename == filnavn0)
        assertThat(vedleggOpplastingResponseList[0].filer[0].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[0].filer[1].filename == filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[1].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[0].filer[2].filename == filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[2].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[1].filer[0].filename == filnavn3)
        assertThat(vedleggOpplastingResponseList[1].filer[0].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[1].filer[1].filename == filnavn4.trim())
        assertThat(vedleggOpplastingResponseList[1].filer[1].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[1].filer[2].filename).startsWith(filnavn5.trim())
        assertThat(vedleggOpplastingResponseList[1].filer[2].status.result == ValidationValues.OK)
    }

    @Test
    fun `sendVedleggTilFiks skal ikke kalle FiksClient hvis ikke alle filene blir validert ok`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val metadata = mutableListOf(
            OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1)), null),
            OpplastetVedleggMetadata(type1, tilleggsinfo1, null, null, mutableListOf(OpplastetFil(filnavn2)), null)
        )
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn0, filtype1, jpgFile),
            MockMultipartFile("files", filnavn1, filtype0, pngFile),
            MockMultipartFile("files", filnavn2, "unknown", ByteArray(0))
        )

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filer[0].filename == filnavn0)
        assertThat(vedleggOpplastingResponseList[0].filer[0].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[0].filer[1].filename == filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[1].status.result == ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[1].filer[0].filename == filnavn2)
        assertThat(vedleggOpplastingResponseList[1].filer[0].status.result == ValidationValues.ILLEGAL_FILE_TYPE)
    }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis filnavn i metadata ikke matcher med filene som sendes`() {
        val metadata = mutableListOf(
            OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil("feilFilnavn.rar")), null),
            OpplastetVedleggMetadata(type1, tilleggsinfo1, null, null, mutableListOf(OpplastetFil(filnavn2)), null)
        )
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn0, filtype1, jpgFile),
            MockMultipartFile("files", filnavn1, filtype0, pngFile),
            MockMultipartFile("files", filnavn2, "unknown", ByteArray(0))
        )

        assertThatExceptionOfType(OpplastingFilnavnMismatchException::class.java)
            .isThrownBy { service.sendVedleggTilFiks(id, files, metadata, "token") }
    }

    @Test
    fun `sendVedleggTilFiks skal ikke gi feilmelding hvis pdf-filen er signert`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }
        every { ettersendelsePdfGenerator.generate(any(), any()) } returns ByteArray(1)

        val filnavn1 = "test1.pdf"
        val filnavn2 = "test2.pdf"
        val filtype = "application/pdf"
        val pdfFile = createPdfByteArray()
        val signedPdfFile = createPdfByteArray(true)

        val metadata = mutableListOf(
            OpplastetVedleggMetadata(
                type0, tilleggsinfo0, null, null,
                mutableListOf(
                    OpplastetFil(filnavn1),
                    OpplastetFil(filnavn2)
                ),
                LocalDate.now()
            )
        )
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn1, filtype, pdfFile),
            MockMultipartFile("files", filnavn2, filtype, signedPdfFile)
        )

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 1) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.OK)
        assertThat(vedleggOpplastingResponseList[0].filer[1].filename).isEqualTo(filnavn2)
        assertThat(vedleggOpplastingResponseList[0].filer[1].status.result).isEqualTo(ValidationValues.OK)
    }

    @Test
    fun `sendVedleggTilFiks skal gi feilmelding hvis pdf-filen er passord-beskyttet`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val filnavn1 = "test1.pdf"
        val filtype = "application/pdf"
        val pdfFile = createPasswordProtectedPdfByteArray()

        val metadata = mutableListOf(
            OpplastetVedleggMetadata(
                type0, tilleggsinfo0, null, null,
                mutableListOf(
                    OpplastetFil(filnavn1)
                ),
                null
            )
        )
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn1, filtype, pdfFile)
        )

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.PDF_IS_ENCRYPTED)
    }

    @Test
    fun `sendVedleggTilFiks skal gi feilmelding hvis bilde er jfif`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val filnavn1 = "test1.jfif"

        val metadata = mutableListOf(
            OpplastetVedleggMetadata(
                type0, tilleggsinfo0, null, null,
                mutableListOf(
                    OpplastetFil(filnavn1)
                ),
                null
            )
        )
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn1, filtype1, jpgFile)
        )

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.ILLEGAL_FILE_TYPE)
    }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis virus er detektert`() {
        every { virusScanner.scan(any(), any()) } throws VirusScanException("mulig virus!", null)

        val metadata = mutableListOf(OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1)), null))
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", filnavn0, filtype1, jpgFile),
            MockMultipartFile("files", filnavn1, filtype0, pngFile)
        )

        assertThatExceptionOfType(VirusScanException::class.java)
            .isThrownBy { service.sendVedleggTilFiks(id, files, metadata, "token") }
    }

    @Test
    fun `skal legge pa UUID pa filnavn`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavn = "fil.pdf"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("fil-$uuid.pdf")
    }

    @Test
    fun `skal legge pa extension pa filnavn uten`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavn = "fil"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("fil-$uuid.pdf")
    }

    @Test
    fun `skal endre extension pa filnavn hvis Tika validerer filen er noe annet enn hva filnavnet tilsier`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavn = "fil.jpg"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("fil-$uuid.pdf")
    }

    @Test
    fun `skal legge pa extension pa filnavn hvis filnavnets extension er ukjent eller ugyldig`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavn = "fil.punktum"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("fil.punktum-$uuid.pdf")
    }

    @Test
    fun `skal kutte ned lange filnavn`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavnUtenExtension50Tegn = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val filnavn = "$filnavnUtenExtension50Tegn-dette-skal-kuttes-bort.pdf"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("$filnavnUtenExtension50Tegn-$uuid.pdf")
    }

    @Test
    fun `skal validere ugyldige tegn i filnavn`() {
        val ugyldigTegn = arrayOf("*", ":", "<", ">", "|", "?", "\\", "/", "â", "اَلْعَرَبِيَّةُ", "blabla?njn")
        for (tegn in ugyldigTegn) {
            assertThat(containsIllegalCharacters(tegn)).isTrue
        }

        val utvalgAvGyldigeTegn = ".aAbBcCdDhHiIjJkKlLmMn   NoOpPqQrRsStTuUvVw...WxXyYzZæÆøØåÅ-_ (),._–-"
        assertThat(containsIllegalCharacters(utvalgAvGyldigeTegn)).isFalse
    }

    @Test
    fun `getOpplastetVedleggMetadataAsString skal returnere antall filer per element i listen`() {
        val metadataList = mutableListOf(
            OpplastetVedleggMetadata(
                "type", "tilleggsinfo", null, null,
                mutableListOf(
                    OpplastetFil("fil1"),
                    OpplastetFil("fil2"),
                    OpplastetFil("fil3")
                ),
                null
            ),
            OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(OpplastetFil("fil4")), null),
            OpplastetVedleggMetadata(
                "type", "tilleggsinfo", null, null,
                mutableListOf(
                    OpplastetFil("fil5"),
                    OpplastetFil("fil6")
                ),
                LocalDate.now()
            )
        )

        assertThat(service.getMetadataAsString(metadataList)).isEqualTo("metadata[0].filer.size: 3, metadata[1].filer.size: 1, metadata[2].filer.size: 2, ")
    }

    @Test
    fun `validateFilenameMatchInMetadataAndFiles skal gi suksess om begge filnavn er like`() {
        val metadataList = mutableListOf(
            OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(OpplastetFil("filnavnet")), null)
        )

        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", "filnavnet", filtype1, jpgFile)
        )

        service.validateFilenameMatchInMetadataAndFiles(metadataList, files)
    }

    @Test
    fun `validateFilenameMatchInMetadataAndFiles skal gi suksess om begge filnavn er tomme`() {
        val metadataList = mutableListOf(
            OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(OpplastetFil("")), null)
        )

        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", null, filtype1, jpgFile)
        )

        service.validateFilenameMatchInMetadataAndFiles(metadataList, files)
    }

    @Test
    fun `validateFilenameMatchInMetadataAndFiles skal gi suksess selv ved leading and trailing whitespaces`() {
        val metadataList = mutableListOf(
            OpplastetVedleggMetadata(
                "type", "tilleggsinfo", null, null,
                mutableListOf(
                    OpplastetFil(" nr 1.jpg"),
                    OpplastetFil("nr 2.jpg "),
                    OpplastetFil("nr 3.jpg"),
                    OpplastetFil("nr 4.jpg"),
                    OpplastetFil("\nnr 5.jpg\t\r"),

                ),
                null
            )
        )

        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", "nr 1.jpg", filtype1, jpgFile),
            MockMultipartFile("files", "nr 2.jpg", filtype1, jpgFile),
            MockMultipartFile("files", " nr 3.jpg", filtype1, jpgFile),
            MockMultipartFile("files", "nr 4.jpg ", filtype1, jpgFile),
            MockMultipartFile("files", "nr 5.jpg", filtype1, jpgFile),
        )

        service.validateFilenameMatchInMetadataAndFiles(metadataList, files)
    }

    @Test
    fun `renameFilenameInMetadataJson skal rename uavhengig av om filnavn er sanitized eller ikke`() {
        val originalFilenameInNFDFormat = "a\u030AA\u030A.pdf" // å/Å på MAC blir lagret som to tegn.
        val newName = "åÅ-1234.pdf"

        val metadataListUtenSanitizedFilename = createSimpleMetadataListWithFilename(originalFilenameInNFDFormat)
        val metadataListUtenSanitizedFilename2 = createSimpleMetadataListWithFilename(originalFilenameInNFDFormat)
        val metadataListMedSanitizedFilename = createSimpleMetadataListWithFilename(sanitizeFileName(originalFilenameInNFDFormat))
        val metadataListMedSanitizedFilename2 = createSimpleMetadataListWithFilename(sanitizeFileName(originalFilenameInNFDFormat))

        service.renameFilenameInMetadataJson(originalFilenameInNFDFormat, newName, metadataListUtenSanitizedFilename)
        service.renameFilenameInMetadataJson(sanitizeFileName(originalFilenameInNFDFormat), newName, metadataListUtenSanitizedFilename2)
        service.renameFilenameInMetadataJson(originalFilenameInNFDFormat, newName, metadataListMedSanitizedFilename)
        service.renameFilenameInMetadataJson(sanitizeFileName(originalFilenameInNFDFormat), newName, metadataListMedSanitizedFilename2)

        assertThat(metadataListUtenSanitizedFilename[0].filer[0].filnavn).isEqualTo(newName)
        assertThat(metadataListUtenSanitizedFilename2[0].filer[0].filnavn).isEqualTo(newName)
        assertThat(metadataListMedSanitizedFilename[0].filer[0].filnavn).isEqualTo(newName)
        assertThat(metadataListMedSanitizedFilename2[0].filer[0].filnavn).isEqualTo(newName)
    }

    @Test
    fun `renameFilenameInMetadataJson skal rename selv om filnavn har leading eller ending whitespaces`() {
        val originalFilenameWithWhitespaces = " \n\t a.pdf\t \r"
        val originalFilenameWithoutWhitespaces = "a.pdf"
        val newName = "a-1234.pdf"

        val metadataListWithWhitespaces = createSimpleMetadataListWithFilename(originalFilenameWithWhitespaces)
        val metadataListWithWhitespaces2 = createSimpleMetadataListWithFilename(originalFilenameWithWhitespaces)
        val metadataListWithoutWhitespaces = createSimpleMetadataListWithFilename(originalFilenameWithoutWhitespaces)
        val metadataListWithoutWhitespaces2 = createSimpleMetadataListWithFilename(originalFilenameWithoutWhitespaces)

        service.renameFilenameInMetadataJson(originalFilenameWithWhitespaces, newName, metadataListWithWhitespaces)
        service.renameFilenameInMetadataJson(originalFilenameWithoutWhitespaces, newName, metadataListWithWhitespaces2)
        service.renameFilenameInMetadataJson(originalFilenameWithWhitespaces, newName, metadataListWithoutWhitespaces)
        service.renameFilenameInMetadataJson(originalFilenameWithoutWhitespaces, newName, metadataListWithoutWhitespaces2)

        assertThat(metadataListWithWhitespaces[0].filer[0].filnavn).isEqualTo(newName)
        assertThat(metadataListWithWhitespaces2[0].filer[0].filnavn).isEqualTo(newName)
        assertThat(metadataListWithoutWhitespaces[0].filer[0].filnavn).isEqualTo(newName)
        assertThat(metadataListWithoutWhitespaces2[0].filer[0].filnavn).isEqualTo(newName)
    }

    private fun createSimpleMetadataListWithFilename(filename: String): MutableList<OpplastetVedleggMetadata> {
        return mutableListOf(
            OpplastetVedleggMetadata(
                "type", "tilleggsinfo", null, null,
                mutableListOf(
                    OpplastetFil(filename)
                ),
                null
            )
        )
    }

    @Test
    fun `getFilnavnListsAsString skal returnere en string med finavn`() {
        val filnavnMetadata = listOf(
            "1",
            "22",
            "333",
            "filæøåÆØÅ",
            "a\u030AA\u030A.pdf",
            "åÅ.pdf"
        )
        val filnavnMultipart = listOf(
            "1",
            "22 ",
            "333",
            "filæøåÆØÅ",
            "åÅ.pdf",
            "åÅ.pdf"
        )

        assertThat(service.getMismatchFilnavnListsAsString(filnavnMetadata, filnavnMultipart))
            .isEqualTo(
                "" +
                    "\r\nFilnavnMetadata : 22 (2 tegn), a\u030AA\u030A.pdf (8 tegn), åÅ.pdf (8 tegn)," +
                    "\r\nFilnavnMultipart: 22  (3 tegn), åÅ.pdf (6 tegn), åÅ.pdf (6 tegn),"
            )
    }

    private fun createImageByteArray(type: String, size: Int = 1): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(size, size, BufferedImage.TYPE_INT_RGB), type, outputStream)
        return outputStream.toByteArray()
    }

    private fun createPdfByteArray(signed: Boolean = false): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = PDDocument()
        document.addPage(PDPage())
        if (signed) {
            document.addSignature(PDSignature())
        }
        document.save(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    private fun createPasswordProtectedPdfByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = PDDocument()
        document.addPage(PDPage())

        val ap = AccessPermission()
        val spp = StandardProtectionPolicy("12345", "secretpw", ap)
        spp.encryptionKeyLength = 256
        spp.permissions = ap
        document.protect(spp)

        document.save(outputStream)
        document.close()
        return outputStream.toByteArray()
    }
}
