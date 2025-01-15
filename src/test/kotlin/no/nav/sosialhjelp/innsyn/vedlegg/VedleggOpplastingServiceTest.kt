package no.nav.sosialhjelp.innsyn.vedlegg

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.exceptions.VirusScanException
import no.nav.sosialhjelp.innsyn.digisosapi.DokumentlagerClient
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.EttersendelsePdfGenerator
import no.nav.sosialhjelp.innsyn.vedlegg.virusscan.VirusScanner
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import org.springframework.mock.web.MockMultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

internal class VedleggOpplastingServiceTest {
    private val fiksClient: FiksClient = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val virusScanner: VirusScanner = mockk()
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val dokumentlagerClient: DokumentlagerClient = mockk()
    private val cacheManager: CacheManager = mockk()
    private val service =
        VedleggOpplastingService(
            fiksClient,
            krypteringService,
            virusScanner,
            ettersendelsePdfGenerator,
            dokumentlagerClient,
            cacheManager,
        )

    private val mockDigisosSak: DigisosSak = mockk(relaxed = true)

    private val id = "123"

    private val type0 = "brukskonto"
    private val tilleggsinfo0 = "kontoutskrift"
    private val filnavn0 = "test0.jpg"
    private val ext0 = ".jpg"
    private val filtype0 = "image/png"
    private val type1 = "bsu"
    private val tilleggsinfo1 = "kontoutskrift"
    private val filnavn1 = "test1.png"
    private val ext1 = ".png"
    private val filtype1 = "image/jpeg"
    private val filnavn2 = "test2.lol"
    private val ext2 = ".lol"

    private val pngFile = createImageByteArray("png")
    private val jpgFile = createImageByteArray("jpg")

    private val mockCertificate: X509Certificate = mockk()

    @BeforeEach
    fun init() {
        clearAllMocks()

        coEvery { fiksClient.hentDigisosSak(any(), any()) } returns mockDigisosSak
        every { mockDigisosSak.fiksDigisosId } returns id
        coEvery { virusScanner.scan(any(), any()) } just runs
        coEvery { dokumentlagerClient.getDokumentlagerPublicKeyX509Certificate() } returns mockCertificate
        coEvery { cacheManager.getCache(any()) } returns null
    }

    @Test
    fun `sendVedleggTilFiks skal kalle FiksClient med gyldige filer for opplasting`() =
        runTest(timeout = 5.seconds) {
            coEvery {
                krypteringService.krypter(any(), any(), any())
            } returns "some test data for my input stream".byteInputStream()
            coEvery { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

            val ettersendelsPdf = ByteArray(1)
            every { ettersendelsePdfGenerator.generate(any(), any()) } returns ettersendelsPdf

            mockkStatic(UUID::class)
            every { UUID.randomUUID().toString() } returns "uuid"

            val largePngFile = createImageByteArray("png", 2)
            val filnavn3 = "test3.png"
            val ext3 = ".png"

            val filnavn4 = " jadda.png"
            val ext4 = " .png"
            val filnavn5 = " uten "
            val ext5 = ""

            val metadata =
                mutableListOf(
                    OpplastetVedleggMetadata(
                        type0,
                        tilleggsinfo0,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn0, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext0, filtype1, jpgFile)
                            },
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext1, filtype0, pngFile)
                            },
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext1, filtype0, largePngFile)
                            },
                        ),
                        null,
                    ),
                    OpplastetVedleggMetadata(
                        type1,
                        tilleggsinfo1,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn3, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext3, filtype0, pngFile)
                            },
                            OpplastetFil(filnavn4, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext4, filtype0, pngFile)
                            },
                            OpplastetFil(filnavn5, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext5, filtype0, pngFile)
                            },
                        ),
                        null,
                    ),
                )
            val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, metadata, "token")

            val filerForOpplastingSlot = slot<List<FilForOpplasting>>()
            val vedleggSpesifikasjonSlot = slot<JsonVedleggSpesifikasjon>()
            coVerify(
                exactly = 1,
            ) { fiksClient.lastOppNyEttersendelse(capture(filerForOpplastingSlot), capture(vedleggSpesifikasjonSlot), any(), any()) }
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
    fun `sendVedleggTilFiks skal ikke kalle FiksClient hvis ikke alle filene blir validert ok`() =
        runTest(timeout = 5.seconds) {
            coEvery {
                krypteringService.krypter(any(), any(), any())
            } returns "some test data for my input stream".byteInputStream()
            coEvery { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

            val metadata =
                mutableListOf(
                    OpplastetVedleggMetadata(
                        type0,
                        tilleggsinfo0,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn0, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext0, filtype1, jpgFile)
                            },
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext1, filtype0, pngFile)
                            },
                        ),
                        null,
                    ),
                    OpplastetVedleggMetadata(
                        type1,
                        tilleggsinfo1,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn2, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext2, "unknown", ByteArray(0))
                            },
                        ),
                        null,
                    ),
                )

            val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, metadata, "token")

            coVerify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

            assertThat(vedleggOpplastingResponseList[0].filer[0].filename == filnavn0)
            assertThat(vedleggOpplastingResponseList[0].filer[0].status.result == ValidationValues.OK)
            assertThat(vedleggOpplastingResponseList[0].filer[1].filename == filnavn1)
            assertThat(vedleggOpplastingResponseList[0].filer[1].status.result == ValidationValues.OK)
            assertThat(vedleggOpplastingResponseList[1].filer[0].filename == filnavn2)
            assertThat(vedleggOpplastingResponseList[1].filer[0].status.result == ValidationValues.ILLEGAL_FILE_TYPE)
        }

    @Test
    fun `sendVedleggTilFiks skal ikke gi feilmelding hvis pdf-filen er signert`() =
        runTest(timeout = 5.seconds) {
            coEvery {
                krypteringService.krypter(any(), any(), any())
            } returns "some test data for my input stream".byteInputStream()
            coEvery { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }
            every { ettersendelsePdfGenerator.generate(any(), any()) } returns ByteArray(1)

            val filnavn1 = "test1.pdf"
            val filnavn2 = "test2.pdf"
            val filtype = "application/pdf"
            val pdfFile = createPdfByteArray()
            val signedPdfFile = createPdfByteArray(true)

            val metadata =
                mutableListOf(
                    OpplastetVedleggMetadata(
                        type0,
                        tilleggsinfo0,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", "${it.uuid}.pdf", filtype, pdfFile)
                            },
                            OpplastetFil(filnavn2, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", "${it.uuid}.pdf", filtype, signedPdfFile)
                            },
                        ),
                        LocalDate.now(),
                    ),
                )

            val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, metadata, "token")

            coVerify(exactly = 1) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

            assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
            assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.OK)
            assertThat(vedleggOpplastingResponseList[0].filer[1].filename).isEqualTo(filnavn2)
            assertThat(vedleggOpplastingResponseList[0].filer[1].status.result).isEqualTo(ValidationValues.OK)
        }

    @Test
    fun `sendVedleggTilFiks skal gi feilmelding hvis pdf-filen er passord-beskyttet`() =
        runTest(timeout = 5.seconds) {
            coEvery {
                krypteringService.krypter(any(), any(), any())
            } returns "some test data for my input stream".byteInputStream()
            coEvery { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

            val filnavn1 = "test1.pdf"
            val filtype = "application/pdf"
            val pdfFile = createPasswordProtectedPdfByteArray()

            val metadata =
                mutableListOf(
                    OpplastetVedleggMetadata(
                        type0,
                        tilleggsinfo0,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil =
                                    MockMultipartFile(
                                        "files",
                                        "${it.uuid}.pdf", filtype, pdfFile,
                                    )
                            },
                        ),
                        null,
                    ),
                )

            val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, metadata, "token")

            coVerify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

            assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
            assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.PDF_IS_ENCRYPTED)
        }

    @Test
    fun `sendVedleggTilFiks skal gi feilmelding hvis bilde er jfif`() =
        runTest(timeout = 5.seconds) {
            coEvery {
                krypteringService.krypter(any(), any(), any())
            } returns "some test data for my input stream".byteInputStream()
            coEvery { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) } answers { nothing }

            val filnavn1 = "test1.jfif"

            val metadata =
                mutableListOf(
                    OpplastetVedleggMetadata(
                        type0,
                        tilleggsinfo0,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil =
                                    MockMultipartFile(
                                        "files",
                                        "${it.uuid}.jfif", filtype1, jpgFile,
                                    )
                            },
                        ),
                        null,
                    ),
                )

            val vedleggOpplastingResponseList = service.sendVedleggTilFiks(id, metadata, "token")

            coVerify(exactly = 0) { fiksClient.lastOppNyEttersendelse(any(), any(), any(), any()) }

            assertThat(vedleggOpplastingResponseList[0].filer[0].filename).isEqualTo(filnavn1)
            assertThat(vedleggOpplastingResponseList[0].filer[0].status.result).isEqualTo(ValidationValues.ILLEGAL_FILE_TYPE)
        }

    @Test
    fun `sendVedleggTilFiks skal kaste exception hvis virus er detektert`() =
        runTest(timeout = 5.seconds) {
            coEvery { virusScanner.scan(any(), any()) } throws VirusScanException("mulig virus!", null)

            val metadata =
                mutableListOf(
                    OpplastetVedleggMetadata(
                        type0,
                        tilleggsinfo0,
                        null,
                        null,
                        mutableListOf(
                            OpplastetFil(filnavn0, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext0, filtype1, jpgFile)
                            },
                            OpplastetFil(filnavn1, UUID.randomUUID()).also {
                                it.fil = MockMultipartFile("files", it.uuid.toString() + ext1, filtype0, pngFile)
                            },
                        ),
                        null,
                    ),
                )

            val runResult = kotlin.runCatching { service.sendVedleggTilFiks(id, metadata, "token") }

            assertThat(runResult.isFailure).isTrue()
            assertThat(runResult.exceptionOrNull()).isInstanceOf(VirusScanException::class.java)
        }

    @Test
    fun `skal legge pa UUID pa filnavn`() {
        val uuid = UUID.fromString("bcd4d076-ff35-49ba-845b-2af021dc7878")

        val filnavn = "fil.pdf"
        val fil =
            OpplastetFil(filnavn, uuid).also {
                it.validering = FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF))
            }
        assertThat(service.createFilename(fil)).isEqualTo("fil-bcd4d076.pdf")
    }

    @Test
    fun `skal legge pa extension pa filnavn uten`() {
        val uuid = UUID.fromString("310db210-95c8-48ec-a00b-287cd2afb434")

        val filnavn = "fil"
        val fil =
            OpplastetFil(filnavn, uuid).also {
                it.validering = FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF))
            }
        assertThat(service.createFilename(fil)).isEqualTo("fil-310db210.pdf")
    }

    @Test
    fun `skal endre extension pa filnavn hvis Tika validerer filen er noe annet enn hva filnavnet tilsier`() {
        val uuid = UUID.fromString("7a8389a3-4cec-43d2-b8ee-f125581e8853")

        val filnavn = "fil.jpg"
        val fil =
            OpplastetFil(filnavn, uuid).also {
                it.validering = FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF))
            }
        assertThat(service.createFilename(fil)).isEqualTo("fil-7a8389a3.pdf")
    }

    @Test
    fun `skal legge pa extension pa filnavn hvis filnavnets extension er ukjent eller ugyldig`() {
        val uuid = UUID.fromString("ddb59040-0c1f-484b-a02d-12b39c951953")

        val filnavn = "fil.punktum"
        val fil =
            OpplastetFil(filnavn, uuid).also {
                it.validering = FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF))
            }
        assertThat(service.createFilename(fil)).isEqualTo("fil.punktum-ddb59040.pdf")
    }

    @Test
    fun `skal kutte ned lange filnavn`() {
        val uuid = UUID.fromString("ad20d5ca-1597-4d48-abae-0bfd116998f0")

        val filnavnUtenExtension50Tegn = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val filnavn = "$filnavnUtenExtension50Tegn-dette-skal-kuttes-bort.pdf"
        val fil =
            OpplastetFil(filnavn, uuid).also {
                it.validering = FilValidering(filnavn, ValidationResult(ValidationValues.OK, TikaFileType.PDF))
            }
        assertThat(service.createFilename(fil)).isEqualTo("$filnavnUtenExtension50Tegn-ad20d5ca.pdf")
    }

    @Test
    fun `skal validere ugyldige tegn i filnavn`() {
        val ugyldigTegn = arrayOf("*", ":", "<", ">", "|", "?", "\\", "/", "â", "اَلْعَرَبِيَّةُ", "blabla?njn")
        for (tegn in ugyldigTegn) {
            assertThat(tegn.containsIllegalCharacters()).isTrue
        }

        val utvalgAvGyldigeTegn = ".aAbBcCdDhHiIjJkKlLmMn   NoOpPqQrRsStTuUvVw...WxXyYzZæÆøØåÅ-_ (),._–-"
        assertThat(utvalgAvGyldigeTegn.containsIllegalCharacters()).isFalse
    }

    private fun createImageByteArray(
        type: String,
        size: Int = 1,
    ): ByteArray {
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
