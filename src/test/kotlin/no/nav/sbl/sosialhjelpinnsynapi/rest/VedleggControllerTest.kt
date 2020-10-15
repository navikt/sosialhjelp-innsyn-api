package no.nav.sbl.sosialhjelpinnsynapi.rest

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.SubjectHandlerUtils
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.config.XsrfGenerator
import no.nav.sbl.sosialhjelpinnsynapi.domain.VedleggResponse
import no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll.TilgangskontrollService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.InternalVedlegg
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggOpplastingService
import no.nav.sbl.sosialhjelpinnsynapi.service.vedlegg.VedleggService
import no.nav.sosialhjelp.api.fiks.DokumentInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import kotlin.test.assertFailsWith

internal class VedleggControllerTest {

    private val vedleggOpplastingService: VedleggOpplastingService = mockk()
    private val vedleggService: VedleggService = mockk()
    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val tilgangskontrollService: TilgangskontrollService = mockk()

    private val controller = VedleggController(vedleggOpplastingService, vedleggService, clientProperties, tilgangskontrollService)

    private val id = "123"

    private val filnavn = "filnavn"
    private val filnavn2 = "filnavn2"
    private val dokumenttype = "type"
    private val tilleggsinfo = "tilleggsinfo"

    private val dokumentlagerId = "id1"
    private val dokumentlagerId2 = "id2"

    private val metadataJson = """[{
  "type": "brukskonto",
  "tilleggsinfo": "kontoutskrift",
  "filer": [{
    "filnavn": "test.jpg"
    }]
  }]"""

    @BeforeEach
    internal fun setUp() {
        clearMocks(vedleggOpplastingService, vedleggService)
        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())

        every { tilgangskontrollService.sjekkTilgang() } just Runs
    }

    @Test
    fun `skal mappe fra InternalVedleggList til VedleggResponseList`() {
        every { vedleggService.hentAlleOpplastedeVedlegg(any(), any()) } returns listOf(
                InternalVedlegg(
                        dokumenttype,
                        tilleggsinfo,
                        listOf(DokumentInfo(filnavn, dokumentlagerId, 123L), DokumentInfo(filnavn2, dokumentlagerId2, 42L)),
                        LocalDateTime.now())
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
        every { vedleggService.hentAlleOpplastedeVedlegg(any(), any()) } returns listOf(
                InternalVedlegg(
                        dokumenttype,
                        null,
                        listOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now),
                InternalVedlegg(
                        dokumenttype,
                        null,
                        listOf(DokumentInfo(filnavn, dokumentlagerId, 123L)),
                        now)
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
                MockMultipartFile("files", "test2.png", null, ByteArray(0)))
        val request: HttpServletRequest = mockk()
        every { request.cookies } returns arrayOf(xsrfCookie(id, "default"))
        assertFailsWith<IllegalStateException> { controller.sendVedlegg(id, files, "token", request) }
    }

    @Test
    fun `skal ikke kaste exception dersom input til sendVedlegg inneholder gyldig metadata-json`() {
        every { vedleggOpplastingService.sendVedleggTilFiks(any(), any(), any(), any()) } returns emptyList()
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
                MockMultipartFile("files", "test.jpg", null, ByteArray(0)))
        val request: HttpServletRequest = mockk()
        every { request.cookies } returns arrayOf(xsrfCookie(id, "default"))
        assertThatCode { controller.sendVedlegg(id, files, "token", request) }.doesNotThrowAnyException()
    }

    @Test
    fun `skal kaste exception dersom token mangler`() {
        every { vedleggOpplastingService.sendVedleggTilFiks(any(), any(), any(), any()) } returns emptyList()
        val files = mutableListOf<MultipartFile>(
                MockMultipartFile("files", "metadata.json", null, metadataJson.toByteArray()),
                MockMultipartFile("files", "test.jpg", null, ByteArray(0)))
        val request: HttpServletRequest = mockk()
        every { request.cookies } returns arrayOf()
        assertFailsWith<IllegalArgumentException> { controller.sendVedlegg(id, files, "token", request) }
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
    fun `skal håndtere filnavn uten extention`() {
        val filnavn = "123"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    @Test
    fun `skal håndtere passe langt filnavn med strek og seks tegn`() {
        val filnavn = "filnavn_som_er_passe_langt-123456.pdf"
        assertThat(controller.removeUUIDFromFilename(filnavn)).isEqualTo(filnavn)
    }

    private fun xsrfCookie(fiksDigisosId: String, token: String): Cookie {

        val xsrfCookie = Cookie("XSRF-TOKEN-INNSYN-API", XsrfGenerator.generateXsrfToken(fiksDigisosId, token))
        xsrfCookie.path = "/"
        xsrfCookie.isHttpOnly = true
        return xsrfCookie
    }

}