package no.nav.sosialhjelp.innsyn.vedlegg

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator
import no.nav.sosialhjelp.innsyn.digisosapi.FiksClient
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

internal class VedleggControllerTest {
    private val vedleggOpplastingService: VedleggOpplastingService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val xsrfGenerator: XsrfGenerator = mockk()
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val digisosSak: DigisosSak = mockk()
    private val model: InternalDigisosSoker = mockk()

    private val controller =
        VedleggController(
            vedleggOpplastingService,
            vedleggService,
            clientProperties,
            tilgangskontroll,
            xsrfGenerator,
            eventService,
            fiksClient,
        )

    private val id = "123"

    private val filnavn = "filnavn"
    private val filnavn2 = "filnavn2"
    private val dokumenttype = "type"
    private val tilleggsinfo = "tilleggsinfo"

    private val dokumentlagerId = "id1"
    private val dokumentlagerId2 = "id2"

    private val metadataJson = """
[{
    "type": "brukskonto",
    "tilleggsinfo": "kontoutskrift",
    "filer": [{
        "filnavn": "test.jpg"
    }]
}]
    """

    @BeforeEach
    internal fun setUp() {
        clearMocks(vedleggOpplastingService, vedleggService)
        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())

        coEvery { tilgangskontroll.sjekkTilgang("token") } just Runs
        every { digisosSak.fiksDigisosId } returns "123"
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    fun `skal mappe fra InternalVedleggList til VedleggResponseList`() =
        runTest(timeout = 5.seconds) {
            coEvery { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSak
            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentAlleOpplastedeVedlegg(any(), any(), any()) } returns
                listOf(
                    InternalVedlegg(
                        dokumenttype,
                        tilleggsinfo,
                        null,
                        null,
                        mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L), DokumentInfo(filnavn2, dokumentlagerId2, 42L)),
                        LocalDateTime.now(),
                        null,
                    ),
                )

            val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id, "token")

            val body = vedleggResponses.body

            assertThat(body).isNotNull
            if (!body.isNullOrEmpty()) {
                assertThat(body).hasSize(2)
                assertThat(body[0].filnavn).isEqualTo(filnavn)
                assertThat(body[0].url).contains(dokumentlagerId)
                assertThat(body[0].storrelse).isEqualTo(123L)

                assertThat(body[1].filnavn).isEqualTo(filnavn2)
                assertThat(body[1].url).contains(dokumentlagerId2)
                assertThat(body[1].storrelse).isEqualTo(42L)
            }
        }

    @Test
    fun `skal utelate duplikater i response`() =
        runTest(timeout = 5.seconds) {
            val now = LocalDateTime.now()
            coEvery { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSak
            coEvery { eventService.createModel(any(), any()) } returns model
            coEvery { vedleggService.hentAlleOpplastedeVedlegg(any(), any(), any()) } returns
                listOf(
                    InternalVedlegg(
                        dokumenttype,
                        null,
                        null,
                        null,
                        mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now,
                        null,
                    ),
                    InternalVedlegg(
                        dokumenttype,
                        null,
                        null,
                        null,
                        mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now,
                        null,
                    ),
                )

            val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id, "token")

            val body = vedleggResponses.body

            assertThat(body).isNotNull
            if (!body.isNullOrEmpty()) {
                assertThat(body).hasSize(1)
                assertThat(body[0].filnavn).isEqualTo(filnavn)
                assertThat(body[0].url).contains(dokumentlagerId)
                assertThat(body[0].storrelse).isEqualTo(123L)
            }
        }

    @Test
    fun `kaster exception dersom input til sendVedlegg ikke inneholder metadata-json`() =
        runTest(timeout = 5.seconds) {
            val files =
                mutableListOf<MultipartFile>(
                    MockMultipartFile("files", "test.jpg", null, ByteArray(0)),
                    MockMultipartFile("files", "test2.png", null, ByteArray(0)),
                )
            val request: HttpServletRequest = mockk()
            every { request.cookies } returns arrayOf(xsrfCookie())
            every { xsrfGenerator.generateXsrfToken(any()) } returns "someRandomChars"
            every { xsrfGenerator.sjekkXsrfToken(any()) } just Runs
            runCatching { controller.sendVedlegg(id, files, "token", request) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            }
        }

    @Test
    fun `skal ikke kaste exception dersom input til sendVedlegg inneholder gyldig metadata-json`() =
        runTest(timeout = 5.seconds) {
            coEvery { vedleggOpplastingService.sendVedleggTilFiks(any(), any(), any()) } returns emptyList()
            val files =
                mutableListOf<MultipartFile>(
                    MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
                    MockMultipartFile("files", "test.jpg", null, ByteArray(0)),
                )
            val request: HttpServletRequest = mockk()
            every { request.cookies } returns arrayOf(xsrfCookie())
            every { xsrfGenerator.generateXsrfToken(any()) } returns "someRandomChars"
            every { xsrfGenerator.sjekkXsrfToken(any()) } just Runs
            assertThat(runCatching { controller.sendVedlegg(id, files, "token", request) }.isSuccess)
        }

    @Test
    fun `skal kaste exception dersom token mangler`() =
        runTest(timeout = 5.seconds) {
            coEvery { vedleggOpplastingService.sendVedleggTilFiks(any(), any(), any()) } returns emptyList()
            val files =
                mutableListOf<MultipartFile>(
                    MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
                    MockMultipartFile("files", "test.jpg", null, ByteArray(0)),
                )
            val request: HttpServletRequest = mockk()
            every { request.cookies } returns arrayOf()
            every { xsrfGenerator.sjekkXsrfToken(any()) } throws IllegalArgumentException()
            runCatching { controller.sendVedlegg(id, files, "token", request) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
            }
        }

    @Test
    fun `skal kaste exception hvis det er filer i metadata som ikke er i resten av filene`() =
        runTest(timeout = 5.seconds) {
            val metadata =
                """
            |[{
            |    "type": "brukskonto",
            |    "tilleggsinfo": "kontoutskrift",
            |    "filer": [{
            |        "filnavn": "test.jpg"
            |    }]
            |}]
            |
                """.trimMargin().toByteArray()

            val request: HttpServletRequest = mockk()
            coEvery { tilgangskontroll.sjekkTilgang("token") } just Runs
            every { xsrfGenerator.sjekkXsrfToken(any()) } throws IllegalArgumentException()

            val files =
                mutableListOf<MultipartFile>(
                    MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
                    MockMultipartFile("files", "test.jpg", null, ByteArray(0)),
                    MockMultipartFile("files", "roflmao.jpg", null, ByteArray(0)),
                )
            runCatching { controller.sendVedlegg(id, files, "token", request) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
            }
        }

    @Test
    fun `skal fjerne UUID fra filnavn dersom dette er satt`() {
        val uuid = "12345678"
        val filnavn = "somefile-$uuid.pdf"

        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo("somefile.pdf")
    }

    @Test
    fun `skal ikke fjerne uuid fra filnavn som er for kort og mangler uuid`() {
        val filnavn = "123.pdf"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    @Test
    fun `skal handtere filnavn uten extension`() {
        val filnavn = "123"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    @Test
    fun `skal handtere passe langt filnavn med strek og seks tegn`() {
        val filnavn = "filnavn_som_er_passe_langt-123456.pdf"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    private fun xsrfCookie(): Cookie {
        val xsrfCookie = Cookie("XSRF-TOKEN-INNSYN-API", "someRandomChars")
        xsrfCookie.path = "/"
        xsrfCookie.isHttpOnly = true
        return xsrfCookie
    }
}
