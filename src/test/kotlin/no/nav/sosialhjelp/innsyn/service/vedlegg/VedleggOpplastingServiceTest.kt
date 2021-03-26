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
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedlegg
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.client.fiks.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.client.virusscan.VirusScanner
import no.nav.sosialhjelp.innsyn.common.OpplastingException
import no.nav.sosialhjelp.innsyn.common.OpplastingFilnavnMismatchException
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


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
        every { dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate(any()) } returns mockCertificate
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

        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1), OpplastetFil(filnavn1)), null),
                OpplastetVedleggMetadata(type1, tilleggsinfo1, null, null, mutableListOf(OpplastetFil(filnavn3)), null))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile),
                MockMultipartFile("files", filnavn1, filtype0, largePngFile),
                MockMultipartFile("files", filnavn3, filtype0, pngFile))

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        val filerForOpplastingSlot = slot<List<FilForOpplasting>>()
        val vedleggSpesifikasjonSlot = slot<JsonVedleggSpesifikasjon>()
        verify(exactly = 1) { fiksClient.lastOppNyEttersendelse(capture(filerForOpplastingSlot), capture(vedleggSpesifikasjonSlot), any(), any()) }
        val filerForOpplasting = filerForOpplastingSlot.captured
        val vedleggSpesifikasjon = vedleggSpesifikasjonSlot.captured

        assertThat(filerForOpplasting).hasSize(5) // Inkluderer ettersendelse.pdf
        assertThat(filerForOpplasting[0].filnavn == filnavn0)
        assertThat(filerForOpplasting[0].mimetype == filtype0)
        assertThat(filerForOpplasting[1].filnavn == filnavn1)
        assertThat(filerForOpplasting[1].mimetype == filtype1)
        assertThat(filerForOpplasting[2].filnavn == filnavn1)
        assertThat(filerForOpplasting[2].mimetype == filtype1)
        assertThat(filerForOpplasting[3].filnavn == filnavn1)
        assertThat(filerForOpplasting[3].mimetype == filtype1)

        assertThat(vedleggSpesifikasjon.vedlegg.size).isEqualTo(2)
        assertThat(vedleggSpesifikasjon.vedlegg[0].type == type0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].tilleggsinfo == tilleggsinfo0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].status == "LastetOpp")
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer.size == 3)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[0].filnavn == filnavn0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[1].filnavn == filnavn1)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[2].filnavn == filnavn1)

        assertThat(vedleggSpesifikasjon.vedlegg[1].type).isEqualTo(type1)
        assertThat(vedleggSpesifikasjon.vedlegg[1].tilleggsinfo).isEqualTo(tilleggsinfo1)
        assertThat(vedleggSpesifikasjon.vedlegg[1].status).isEqualTo("LastetOpp")
        assertThat(vedleggSpesifikasjon.vedlegg[1].filer.size).isEqualTo(1)
        assertThat(vedleggSpesifikasjon.vedlegg[1].filer[0].filnavn.replace("-uuid", "")).isEqualTo(filnavn3)

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
    }

    @Test
    fun `sendVedleggTilFiks skal ikke kalle FiksClient hvis ikke alle filene blir validert ok`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1)), null),
                OpplastetVedleggMetadata(type1, tilleggsinfo1, null, null, mutableListOf(OpplastetFil(filnavn2)), null))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile),
                MockMultipartFile("files", filnavn2, "unknown", ByteArray(0)))

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
                OpplastetVedleggMetadata(type1, tilleggsinfo1, null, null, mutableListOf(OpplastetFil(filnavn2)), null))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile),
                MockMultipartFile("files", filnavn2, "unknown", ByteArray(0)))

        assertFailsWith<OpplastingFilnavnMismatchException> { service.sendVedleggTilFiks(id, files, metadata, "token") }
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
                OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(
                        OpplastetFil(filnavn1),
                        OpplastetFil(filnavn2)), LocalDate.now()))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn1, filtype, pdfFile),
                MockMultipartFile("files", filnavn2, filtype, signedPdfFile))

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
                OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(
                        OpplastetFil(filnavn1)), null))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn1, filtype, pdfFile))

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
        assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.PDF_IS_ENCRYPTED)
    }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis virus er detektert`() {
        every { virusScanner.scan(any(), any()) } throws OpplastingException("mulig virus!", null)

        val metadata = mutableListOf(OpplastetVedleggMetadata(type0, tilleggsinfo0, null, null, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1)), null))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile))

        assertThatExceptionOfType(OpplastingException::class.java)
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
    fun `skal legge pa extention pa filnavn uten`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavn = "fil"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("fil-$uuid.pdf")
    }

    @Test
    fun `skal legge pa extention pa filnavn bare dersom det mangler`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns uuid

        val filnavn = "fil.jpg"
        val valideringer = listOf(FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF)))
        assertThat(service.createFilename(filnavn, valideringer)).isEqualTo("fil-$uuid.jpg")
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
            assertTrue { containsIllegalCharacters(tegn) }
        }

        val utvalgAvGyldigeTegn = ".aAbBcCdDhHiIjJkKlLmMn   NoOpPqQrRsStTuUvVw...WxXyYzZæÆøØåÅ-_ (),._–-"
        assertFalse { containsIllegalCharacters(utvalgAvGyldigeTegn) }
    }

    @Test
    fun `vedleggJson skal ikke ha hendelsetype og hendelsereferanse om featureToggle er inaktiv`() {
        every { unleashClient.isEnabled(any(), false) } returns false

        val opplastetVedleggMetadata = OpplastetVedleggMetadata(
                "type",
                "tilleggsinfo",
                JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT,
                "hendelsereferanse",
                mutableListOf(OpplastetFil("fil1")),
                null
        );

        val createJsonVedlegg = service.createJsonVedlegg(opplastetVedleggMetadata, emptyList())!!

        assertNull(createJsonVedlegg.hendelseType)
        assertNull(createJsonVedlegg.hendelseReferanse)
    }

    @Test
    fun `vedleggJson skal ha hendelsetype og hendelsereferanse om featureToggle er aktiv`() {
        every { unleashClient.isEnabled(any(), false) } returns true

        val opplastetVedleggMetadata = OpplastetVedleggMetadata(
                "type",
                "tilleggsinfo",
                JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT,
                "hendelsereferanse",
                mutableListOf(OpplastetFil("fil1")),
                null
        );

        val createJsonVedlegg = service.createJsonVedlegg(opplastetVedleggMetadata, emptyList())!!

        assertEquals(JsonVedlegg.HendelseType.DOKUMENTASJON_ETTERSPURT, createJsonVedlegg.hendelseType)
        assertEquals("hendelsereferanse", createJsonVedlegg.hendelseReferanse)
    }

    @Test
    fun `getOpplastetVedleggMetadataAsString skal returnere antall filer per element i listen`() {
        val metadataList = mutableListOf(
                OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(
                        OpplastetFil("fil1"),
                        OpplastetFil("fil2"),
                        OpplastetFil("fil3")
                ), null),
                OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(OpplastetFil("fil4")), null),
                OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(
                        OpplastetFil("fil5"),
                        OpplastetFil("fil6")
                ), LocalDate.now())
        )

        assertEquals("metadata[0].filer.size: 3, metadata[1].filer.size: 1, metadata[2].filer.size: 2, ", service.getMetadataAsString(metadataList))
    }

    @Test
    fun `renameFilenameInMetadataJson skal rename uavhengig av om filnavn er sanitized eller ikke`() {
        val originalFilenameInNFDFormat ="a\u030AA\u030A.pdf" // å/Å på MAC blir lagret som to tegn.
        val newName ="åÅ-1234.pdf"

        val metadataListUtenSanitizedFilename = createSimpleMetadataListWithFilename(originalFilenameInNFDFormat)
        val metadataListUtenSanitizedFilename2 = createSimpleMetadataListWithFilename(originalFilenameInNFDFormat)
        val metadataListMedSanitizedFilename = createSimpleMetadataListWithFilename(sanitizeFileName(originalFilenameInNFDFormat))
        val metadataListMedSanitizedFilename2 = createSimpleMetadataListWithFilename(sanitizeFileName(originalFilenameInNFDFormat))

        service.renameFilenameInMetadataJson(originalFilenameInNFDFormat, newName, metadataListUtenSanitizedFilename)
        service.renameFilenameInMetadataJson(sanitizeFileName(originalFilenameInNFDFormat), newName, metadataListUtenSanitizedFilename2)
        service.renameFilenameInMetadataJson(originalFilenameInNFDFormat, newName, metadataListMedSanitizedFilename)
        service.renameFilenameInMetadataJson(sanitizeFileName(originalFilenameInNFDFormat), newName, metadataListMedSanitizedFilename2)

        assertEquals(newName, metadataListUtenSanitizedFilename[0].filer[0].filnavn)
        assertEquals(newName, metadataListUtenSanitizedFilename2[0].filer[0].filnavn)
        assertEquals(newName, metadataListMedSanitizedFilename[0].filer[0].filnavn)
        assertEquals(newName, metadataListMedSanitizedFilename2[0].filer[0].filnavn)
    }

    private fun createSimpleMetadataListWithFilename(filename: String): MutableList<OpplastetVedleggMetadata> {
        return mutableListOf(
                OpplastetVedleggMetadata("type", "tilleggsinfo", null, null, mutableListOf(
                        OpplastetFil(filename)), null
                )
        )
    }
    @Test
    fun `getFilnavnListsAsString skal returnere en string med finavn`() {
        val filnavnMetadata = listOf(
                "1",
                "22",
                "333",
                "fil4",
                "a\u030AA\u030A.pdf",
                "åÅ.pdf"
        )
        val filnavnMultipart = listOf(
                "1",
                "22",
                "333",
                "fil4",
                "åÅ.pdf",
                "åÅ.pdf"
        )

        assertEquals("" +
                "\r\nFilnavnMetadata : a\u030AA\u030A.pdf, åÅ.pdf," +
                "\r\nFilnavnMultipart: åÅ.pdf, åÅ.pdf,",
                service.getMismatchFilnavnListsAsString(filnavnMetadata, filnavnMultipart))
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
