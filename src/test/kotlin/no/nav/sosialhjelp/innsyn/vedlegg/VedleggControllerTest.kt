package no.nav.sosialhjelp.innsyn.vedlegg

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import no.nav.sosialhjelp.innsyn.app.ClientProperties
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator
import no.nav.sosialhjelp.innsyn.client.fiks.FiksClient
import no.nav.sosialhjelp.innsyn.common.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.domain.InternalDigisosSoker
import no.nav.sosialhjelp.innsyn.event.EventService
import no.nav.sosialhjelp.innsyn.tilgang.Tilgangskontroll
import no.nav.sosialhjelp.innsyn.vedlegg.dto.VedleggResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

internal class VedleggControllerTest {

    private val vedleggOpplastingService: VedleggOpplastingService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val tilgangskontroll: Tilgangskontroll = mockk()
    private val xsrfGenerator: XsrfGenerator = mockk()
    private val eventService: EventService = mockk()
    private val fiksClient: FiksClient = mockk()
    private val digisosSak: DigisosSak = mockk()
    private val model: InternalDigisosSoker = mockk()

    private val controller =
        VedleggController(vedleggOpplastingService, vedleggService, clientProperties, tilgangskontroll, xsrfGenerator, eventService, fiksClient)

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

        every { tilgangskontroll.sjekkTilgang("token") } just Runs
        every { digisosSak.fiksDigisosId } returns "123"
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    fun `skal mappe fra InternalVedleggList til VedleggResponseList`() {
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSak
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentAlleOpplastedeVedlegg(any(), any(), any()) } returns listOf(
            InternalVedlegg(
                dokumenttype,
                tilleggsinfo,
                null,
                null,
                mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L), DokumentInfo(filnavn2, dokumentlagerId2, 42L)),
                LocalDateTime.now(),
                null
            )
        )

        val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id, "token")

        val body = vedleggResponses.body

        assertThat(body).isNotNull
        if (body != null && body.isNotEmpty()) {
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
    fun `skal utelate duplikater i response`() {
        val now = LocalDateTime.now()
        every { fiksClient.hentDigisosSak(any(), any(), any()) } returns digisosSak
        every { eventService.createModel(any(), any()) } returns model
        every { vedleggService.hentAlleOpplastedeVedlegg(any(), any(), any()) } returns listOf(
            InternalVedlegg(
                dokumenttype,
                null,
                null,
                null,
                mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                now,
                null
            ),
            InternalVedlegg(
                dokumenttype,
                null,
                null,
                null,
                mutableListOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                now,
                null
            )
        )

        val vedleggResponses: ResponseEntity<List<VedleggResponse>> = controller.hentVedlegg(id, "token")

        val body = vedleggResponses.body

        assertThat(body).isNotNull
        if (body != null && body.isNotEmpty()) {
            assertThat(body).hasSize(1)
            assertThat(body[0].filnavn).isEqualTo(filnavn)
            assertThat(body[0].url).contains(dokumentlagerId)
            assertThat(body[0].storrelse).isEqualTo(123L)
        }
    }

    @Test
    fun `kaster exception dersom input til sendVedlegg ikke inneholder metadata-json`() {
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", "test.jpg", null, ByteArray(0)),
            MockMultipartFile("files", "test2.png", null, ByteArray(0))
        )
        val request: HttpServletRequest = mockk()
        every { request.cookies } returns arrayOf(xsrfCookie())
        every { xsrfGenerator.generateXsrfToken(any()) } returns "someRandomChars"
        every { xsrfGenerator.sjekkXsrfToken(any()) } just Runs
        assertThatExceptionOfType(IllegalStateException::class.java)
            .isThrownBy { controller.sendVedlegg(id, files, "token", request) }
    }

    @Test
    fun `skal ikke kaste exception dersom input til sendVedlegg inneholder gyldig metadata-json`() {
        every { vedleggOpplastingService.sendVedleggTilFiks(any(), any(), any(), any()) } returns emptyList()
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
            MockMultipartFile("files", "test.jpg", null, ByteArray(0))
        )
        val request: HttpServletRequest = mockk()
        every { request.cookies } returns arrayOf(xsrfCookie())
        every { xsrfGenerator.generateXsrfToken(any()) } returns "someRandomChars"
        every { xsrfGenerator.sjekkXsrfToken(any()) } just Runs
        assertThatCode { controller.sendVedlegg(id, files, "token", request) }.doesNotThrowAnyException()
    }

    @Test
    fun `skal kaste exception dersom token mangler`() {
        every { vedleggOpplastingService.sendVedleggTilFiks(any(), any(), any(), any()) } returns emptyList()
        val files = mutableListOf<MultipartFile>(
            MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
            MockMultipartFile("files", "test.jpg", null, ByteArray(0))
        )
        val request: HttpServletRequest = mockk()
        every { request.cookies } returns arrayOf()
        every { xsrfGenerator.sjekkXsrfToken(any()) } throws IllegalArgumentException()
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { controller.sendVedlegg(id, files, "token", request) }
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
