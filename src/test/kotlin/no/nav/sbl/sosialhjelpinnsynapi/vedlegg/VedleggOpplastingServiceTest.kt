package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.*
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingException
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingFilnavnMismatchException
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.redis.CacheProperties
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisStore
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetFil
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.virusscan.VirusScanner
import org.apache.commons.io.IOUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.test.assertFailsWith

internal class VedleggOpplastingServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val virusScanner: VirusScanner = mockk()
    private val redisStore: RedisStore = mockk()
    private val cacheProperties: CacheProperties = mockk(relaxed = true)
    private val service = VedleggOpplastingService(fiksClient, krypteringService, virusScanner, redisStore, cacheProperties)

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

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.fiksDigisosId } returns id
        every { virusScanner.scan(any(), any()) } just runs
        every { redisStore.set(any(), any(), any()) } returns "OK"
    }

    @Test
    fun `sendVedleggTilFiks skal kalle FiksClient med gyldige filer for opplasting`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString()} returns "uuid"

        val largePngFile = createImageByteArray("png", 2)
        val filnavn3 = "test3.png"

        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1), OpplastetFil(filnavn1))),
                OpplastetVedleggMetadata(type1, tilleggsinfo1, mutableListOf(OpplastetFil(filnavn3))))
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

        assertThat(filerForOpplasting).hasSize(4)
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

        assertThat(vedleggOpplastingResponseList[0].filnavn == filnavn0)
        assertThat(vedleggOpplastingResponseList[0].status == "OK")
        assertThat(vedleggOpplastingResponseList[1].filnavn == filnavn1)
        assertThat(vedleggOpplastingResponseList[1].status == "OK")
        assertThat(vedleggOpplastingResponseList[2].filnavn == filnavn1)
        assertThat(vedleggOpplastingResponseList[2].status == "OK")
        assertThat(vedleggOpplastingResponseList[3].filnavn == filnavn3)
        assertThat(vedleggOpplastingResponseList[2].status == "OK")
    }

    @Test
    fun `sendVedleggTilFiks skal ikke kalle FiksClient hvis ikke alle filene blir validert ok`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1))),
                OpplastetVedleggMetadata(type1, tilleggsinfo1, mutableListOf(OpplastetFil(filnavn2))))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile),
                MockMultipartFile("files", filnavn2, "unknown", ByteArray(0)))

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filnavn == filnavn0)
        assertThat(vedleggOpplastingResponseList[0].status == "OK")
        assertThat(vedleggOpplastingResponseList[1].filnavn == filnavn1)
        assertThat(vedleggOpplastingResponseList[1].status == "OK")
        assertThat(vedleggOpplastingResponseList[2].filnavn == filnavn2)
        assertThat(vedleggOpplastingResponseList[2].status == MESSAGE_ILLEGAL_FILE_TYPE)
    }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis filnavn i metadata ikke matcher med filene som sendes`() {
        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(OpplastetFil(filnavn0), OpplastetFil("feilFilnavn.rar"))),
                OpplastetVedleggMetadata(type1, tilleggsinfo1, mutableListOf(OpplastetFil(filnavn2))))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile),
                MockMultipartFile("files", filnavn2, "unknown", ByteArray(0)))

        assertFailsWith<OpplastingFilnavnMismatchException>{ service.sendVedleggTilFiks(id, files, metadata, "token") }
    }

    @Test
    fun `sendVedleggTilFiks skal gi feilmelding hvis pdf-filen er signert`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val filnavn1 = "test1.pdf"
        val filnavn2 = "test2.pdf"
        val filtype = "application/pdf"
        val pdfFile = createPdfByteArray()
        val signedPdfFile = createPdfByteArray(true)

        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(
                        OpplastetFil(filnavn1),
                        OpplastetFil(filnavn2))))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn1, filtype, pdfFile),
                MockMultipartFile("files", filnavn2, filtype, signedPdfFile))

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        verify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

        assertThat(vedleggOpplastingResponseList[0].filnavn).isEqualTo(filnavn1)
        assertThat(vedleggOpplastingResponseList[0].status).isEqualTo("OK")
        assertThat(vedleggOpplastingResponseList[1].filnavn).isEqualTo(filnavn2)
        assertThat(vedleggOpplastingResponseList[1].status).isEqualTo(MESSAGE_PDF_IS_SIGNED)
    }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis virus er detektert`() {
        every { virusScanner.scan(any(), any()) } throws OpplastingException("mulig virus!", null)

        val metadata = mutableListOf(OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1))))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile))

        assertThatExceptionOfType(OpplastingException::class.java)
                .isThrownBy { service.sendVedleggTilFiks(id, files, metadata, "token") }
    }

    @Test
    fun `skal legge på UUID på filnavn`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString()} returns uuid

        val filnavn = "fil.pdf"
        assertThat(service.createFilename(filnavn, "application/pdf")).isEqualTo("fil-$uuid.pdf")
    }

    @Test
    fun `skal kutte ned lange filnavn`() {
        val uuid = "12345678"
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString()} returns uuid

        val filnavnUtenExtension50Tegn = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val filnavn = "$filnavnUtenExtension50Tegn-dette-skal-kuttes-bort.pdf"
        assertThat(service.createFilename(filnavn, "application/pdf")).isEqualTo("$filnavnUtenExtension50Tegn-$uuid.pdf")
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
}