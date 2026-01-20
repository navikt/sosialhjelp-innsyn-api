package no.nav.sosialhjelp.innsyn.digisosapi

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_minimal_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.sosialhjelpJsonMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
import no.nav.sosialhjelp.innsyn.vedlegg.Filename
import no.nav.sosialhjelp.innsyn.vedlegg.KrypteringService
import no.nav.sosialhjelp.innsyn.vedlegg.pdf.EttersendelsePdfGenerator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.io.InputStream
import kotlin.time.Duration.Companion.seconds

internal class FiksServiceTest {
    private val mockWebServer = MockWebServer()
    private val fiksWebClient = WebClient.create(mockWebServer.url("/").toString())

    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val meterRegistry: MeterRegistry = mockk()
    private val counterMock: Counter = mockk()
    private val fiksClient = FiksClient(fiksWebClient, tilgangskontroll, 2, 5)
    private lateinit var fiksService: FiksService

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()
        mockkObject(TokenUtils)

        coEvery { TokenUtils.getToken() } returns Token("token")

        coEvery { tilgangskontroll.verifyDigisosSakIsForCorrectUser(any()) } just Runs

        every { meterRegistry.counter(any()) } returns counterMock
        every { counterMock.increment() } just Runs

        fiksService = FiksService(tilgangskontroll, fiksClient, meterRegistry)
    }

    @AfterEach
    internal fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `GET eksakt 1 DigisosSak`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_digisossak_response),
            )

            val result = fiksService.getSoknad(id)

            assertThat(result).isNotNull
        }

    @Test
    fun `GET DigisosSak skal bruke retry hvis Fiks gir 5xx-feil`() =
        runTest(timeout = 10.seconds) {
            repeat(3) {
                mockWebServer.enqueue(
                    MockResponse()
                        .setResponseCode(500),
                )
            }

            val result = kotlin.runCatching { fiksService.getSoknad(id) }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(FiksServerException::class.java)
            assertThat(mockWebServer.requestCount).isEqualTo(3)
        }

    @Test
    fun `GET alle DigisosSaker skal bruke retry hvis Fiks gir 5xx-feil`() =
        runTest(timeout = 20.seconds) {
            repeat(3) {
                mockWebServer.enqueue(
                    MockResponse()
                        .setResponseCode(500),
                )
            }

            val result = kotlin.runCatching { fiksService.getAllSoknader() }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(FiksServerException::class.java)
            assertThat(mockWebServer.requestCount).isEqualTo(3)
        }

    @Test
    fun `GET alle DigisosSaker skal ikke bruke retry hvis Fiks gir 4xx-feil`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(400),
            )

            val result = kotlin.runCatching { fiksService.getAllSoknader() }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(FiksClientException::class.java)
            assertThat(mockWebServer.requestCount).isEqualTo(1)
        }

    @Test
    fun `GET alle DigisosSaker`() =
        runTest(timeout = 5.seconds) {
            val digisosSakOk = sosialhjelpJsonMapper.readValue(ok_digisossak_response, DigisosSak::class.java)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(sosialhjelpJsonMapper.writeValueAsString(listOf(digisosSakOk, digisosSakOk))),
            )

            val result = fiksService.getAllSoknader()

            assertThat(result).isNotNull
            assertThat(result).hasSize(2)
        }

    @Test
    fun `GET dokument`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_minimal_jsondigisossoker_response),
            )

            val result = fiksService.getDocument(id, "dokumentlagerId", JsonDigisosSoker::class.java)

            assertThat(result).isNotNull
        }

    @Test
    fun `POST ny ettersendelse`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_minimal_jsondigisossoker_response),
            )
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("true"))

            val fil1: InputStream = mockk()
            val fil2: InputStream = mockk()

            val ettersendelsPdf = ByteArray(1)
            every { ettersendelsePdfGenerator.generate(any(), any()) } returns ettersendelsPdf
            coEvery { krypteringService.krypter(any(), any(), any()) } returns fil1

            val files =
                listOf(
                    FilForOpplasting(Filename("filnavn0"), "image/png", 1L, fil1),
                    FilForOpplasting(Filename("filnavn1"), "image/jpg", 1L, fil2),
                )

            runCatching {
                fiksService.uploadEttersendelse(
                    files,
                    JsonVedleggSpesifikasjon(),
                    id,
                )
            }.let { assertThat(it.isSuccess) }
        }

    @Test
    internal fun `should produce body for upload`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()

        val files =
            listOf(
                FilForOpplasting(Filename("filnavn0"), "image/png", 1L, fil1),
                FilForOpplasting(Filename("filnavn1"), "image/jpg", 1L, fil2),
            )
        val body = fiksService.createBodyForUpload(JsonVedleggSpesifikasjon(), files)

        assertThat(body.size == 5)
        assertThat(body.keys.contains("vedlegg.json"))
        assertThat(body.keys.contains("vedleggSpesifikasjon:0"))
        assertThat(body.keys.contains("dokument:0"))
        assertThat(body.keys.contains("vedleggSpesifikasjon:1"))
        assertThat(body.keys.contains("dokument:1"))
        assertThat(body["dokument:0"].toString().contains("InputStream resource"))
        assertThat(body["dokument:1"].toString().contains("InputStream resource"))
        assertThat(body["vedlegg.json"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(body["vedleggSpesifikasjon:0"].toString().contains("text/plain;charset=UTF-8"))
        assertThat(body["vedleggSpesifikasjon:1"].toString().contains("text/plain;charset=UTF-8"))
    }
}
