package no.nav.sbl.sosialhjelpinnsynapi.vedlegg

import io.mockk.*
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sbl.sosialhjelpinnsynapi.common.OpplastingException
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.fiks.FiksClient
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetFil
import no.nav.sbl.sosialhjelpinnsynapi.rest.OpplastetVedleggMetadata
import no.nav.sbl.sosialhjelpinnsynapi.virusscan.VirusScanner
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertFailsWith

internal class VedleggOpplastingServiceTest {

    private val fiksClient: FiksClient = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val virusScanner: VirusScanner = mockk()
    private val service = VedleggOpplastingService(fiksClient, krypteringService, virusScanner)

    private val mockDigisosSak: DigisosSak = mockk()

    private val id = "123"
    private val kommunenummer = "1337"

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
        clearMocks(fiksClient, mockDigisosSak)

        every { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.kommunenummer } returns kommunenummer
        every { virusScanner.scan(any(), any()) } just runs
    }

    @Test
    fun `sendVedleggTilFiks skal kalle FiksClient med gyldige filer for opplasting`() {
        every { krypteringService.krypter(any(), any(), any()) } returns IOUtils.toInputStream("some test data for my input stream", "UTF-8")
        every { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

        val metadata = mutableListOf(OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1))))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile))

        val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, files, metadata, "token")

        val filerForOpplastingSlot = slot<List<FilForOpplasting>>()
        val vedleggSpesifikasjonSlot = slot<JsonVedleggSpesifikasjon>()
        verify(exactly = 1) { fiksClient.lastOppNyEttersendelse(capture(filerForOpplastingSlot), capture(vedleggSpesifikasjonSlot), any(), any()) }
        val filerForOpplasting = filerForOpplastingSlot.captured
        val vedleggSpesifikasjon = vedleggSpesifikasjonSlot.captured

        assertThat(filerForOpplasting).hasSize(2)
        assertThat(filerForOpplasting[0].filnavn == filnavn0)
        assertThat(filerForOpplasting[0].mimetype == filtype0)
        assertThat(filerForOpplasting[1].filnavn == filnavn1)
        assertThat(filerForOpplasting[1].mimetype == filtype1)

        assertThat(vedleggSpesifikasjon.vedlegg.size == 1)
        assertThat(vedleggSpesifikasjon.vedlegg[0].type == type0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].tilleggsinfo == tilleggsinfo0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].status == "LastetOpp")
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer.size == 2)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[0].filnavn == filnavn0)
        assertThat(vedleggSpesifikasjon.vedlegg[0].filer[1].filnavn == filnavn1)

        assertThat(vedleggOpplastingResponseList[0].filnavn == filnavn0)
        assertThat(vedleggOpplastingResponseList[0].status == "OK")
        assertThat(vedleggOpplastingResponseList[1].filnavn == filnavn1)
        assertThat(vedleggOpplastingResponseList[1].status == "OK")
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

        assertFailsWith<IllegalStateException>{ service.sendVedleggTilFiks(id, files, metadata, "token") }
    }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis filnavn ikke er unike`() {
        val metadata = mutableListOf(
                OpplastetVedleggMetadata(type0, tilleggsinfo0, mutableListOf(OpplastetFil(filnavn0), OpplastetFil(filnavn1))),
                OpplastetVedleggMetadata(type1, tilleggsinfo1, mutableListOf(OpplastetFil(filnavn2), OpplastetFil(filnavn2))))
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", filnavn0, filtype1, jpgFile),
                MockMultipartFile("files", filnavn1, filtype0, pngFile),
                MockMultipartFile("files", filnavn2, "unknown", ByteArray(0)),
                MockMultipartFile("files", filnavn2, "unknown", ByteArray(0)))

        assertFailsWith<IllegalStateException>{ service.sendVedleggTilFiks(id, files, metadata, "token") }
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

    private fun createImageByteArray(type: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), type, outputStream)
        return outputStream.toByteArray()
    }
}