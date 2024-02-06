package no.nav.sosialhjelp.innsyn.digisosapi

import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.vedlegg.JsonVedleggSpesifikasjon
import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.api.fiks.exceptions.FiksClientException
import no.nav.sosialhjelp.api.fiks.exceptions.FiksServerException
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.responses.ok_digisossak_response
import no.nav.sosialhjelp.innsyn.responses.ok_minimal_jsondigisossoker_response
import no.nav.sosialhjelp.innsyn.tilgang.TilgangskontrollService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import no.nav.sosialhjelp.innsyn.vedlegg.FilForOpplasting
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

internal class FiksClientTest {
    private val mockWebServer = MockWebServer()
    private val fiksWebClient = WebClient.create(mockWebServer.url("/").toString())

    private val redisService: RedisService = mockk()
    private val ettersendelsePdfGenerator: EttersendelsePdfGenerator = mockk()
    private val krypteringService: KrypteringService = mockk()
    private val tilgangskontroll: TilgangskontrollService = mockk()
    private val meterRegistry: MeterRegistry = mockk()
    private val counterMock: Counter = mockk()
    private lateinit var fiksClient: FiksClientImpl

    private val id = "123"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisService.get<Any>(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs
        every { redisService.defaultTimeToLiveSeconds } returns 1

        coEvery { tilgangskontroll.verifyDigisosSakIsForCorrectUser(any()) } just Runs

        every { meterRegistry.counter(any()) } returns counterMock
        every { counterMock.increment() } just Runs

        fiksClient = FiksClientImpl(fiksWebClient, tilgangskontroll, redisService, 2L, 5L, 10L, meterRegistry)
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

            val result = fiksClient.hentDigisosSak(id, "Token", false)

            assertThat(result).isNotNull
        }

    @Test
    fun `GET digisosSak fra cache`() =
        runTest(timeout = 5.seconds) {
            val digisosSak = objectMapper.readValue<DigisosSak>(ok_digisossak_response)
            every { redisService.get(id, DigisosSak::class.java) } returns digisosSak

            val result2 = fiksClient.hentDigisosSak(id, "Token", true)

            assertThat(result2).isNotNull

            verify(exactly = 0) { redisService.put(any(), any(), any()) }
        }

    @Test
    fun `GET digisosSak fra cache etter put`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_digisossak_response),
            )

            val result1 = fiksClient.hentDigisosSak(id, "Token", true)

            assertThat(result1).isNotNull
            verify(exactly = 1) { redisService.put(any(), any(), any()) }
            verify(exactly = 1) { redisService.get(any(), DigisosSak::class.java) }

            val digisosSak: DigisosSak = objectMapper.readValue(ok_digisossak_response)
            every { redisService.get(id, DigisosSak::class.java) } returns digisosSak

            val result = fiksClient.hentDigisosSak(id, "Token", true)

            assertThat(result).isNotNull

            verify(exactly = 1) { redisService.put(any(), any(), any()) }
            verify(exactly = 2) { redisService.get<Any>(any(), any()) }
        }

    @Test
    fun `GET DigisosSak skal bruke retry hvis Fiks gir 5xx-feil`() =
        runTest(timeout = 5.seconds) {
            repeat(3) {
                mockWebServer.enqueue(
                    MockResponse()
                        .setResponseCode(500),
                )
            }

            val result = kotlin.runCatching { fiksClient.hentDigisosSak(id, "Token", true) }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(FiksServerException::class.java)
            assertThat(mockWebServer.requestCount).isEqualTo(3)
        }

    @Test
    fun `GET alle DigisosSaker skal bruke retry hvis Fiks gir 5xx-feil`() =
        runTest(timeout = 5.seconds) {
            repeat(3) {
                mockWebServer.enqueue(
                    MockResponse()
                        .setResponseCode(500),
                )
            }

            val result = kotlin.runCatching { fiksClient.hentAlleDigisosSaker("Token") }
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

            val result = kotlin.runCatching { fiksClient.hentAlleDigisosSaker("Token") }
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(FiksClientException::class.java)
            assertThat(mockWebServer.requestCount).isEqualTo(1)
        }

    @Test
    fun `GET alle DigisosSaker`() =
        runTest(timeout = 5.seconds) {
            val digisosSakOk = objectMapper.readValue(ok_digisossak_response, DigisosSak::class.java)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(objectMapper.writeValueAsString(listOf(digisosSakOk, digisosSakOk))),
            )

            val result = fiksClient.hentAlleDigisosSaker("Token")

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

            val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

            assertThat(result).isNotNull
        }

    @Test
    fun `GET dokument fra cache`() =
        runTest(timeout = 5.seconds) {
            val jsonDigisosSoker = objectMapper.readValue<JsonDigisosSoker>(ok_minimal_jsondigisossoker_response)
            every { redisService.get(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

            val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

            assertThat(result2).isNotNull

            verify(exactly = 0) { redisService.put(any(), any(), any()) }
        }

    @Test
    fun `GET dokument fra cache etter put`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_minimal_jsondigisossoker_response),
            )

            val result1 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

            assertThat(result1).isNotNull
            verify(exactly = 1) { redisService.put(any(), any(), any()) }
            verify(exactly = 1) { redisService.get(any(), JsonDigisosSoker::class.java) }

            val jsonDigisosSoker = objectMapper.readValue<JsonDigisosSoker>(ok_minimal_jsondigisossoker_response)
            every { redisService.get(any(), JsonDigisosSoker::class.java) } returns jsonDigisosSoker

            val result = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

            assertThat(result).isNotNull

            verify(exactly = 1) { redisService.put(any(), any(), any()) }
            verify(exactly = 2) { redisService.get(any(), JsonDigisosSoker::class.java) }
        }

    @Test
    fun `GET dokument - get fra cache returnerer feil type`() =
        runTest(timeout = 5.seconds) {
            // cache returnerer jsonsoknad, men vi forventer jsondigisossoker
            every { redisService.get(any(), JsonDigisosSoker::class.java) } returns null

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_minimal_jsondigisossoker_response),
            )

            val result2 = fiksClient.hentDokument(id, "dokumentlagerId", JsonDigisosSoker::class.java, "Token")

            assertThat(result2).isNotNull

            verify(exactly = 1) { redisService.put(any(), any(), any()) }
        }

    @Test
    fun `POST ny ettersendelse`() =
        runTest(timeout = 5.seconds) {
            every { redisService.get(any(), JsonDigisosSoker::class.java) } returns null

            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_minimal_jsondigisossoker_response),
            )
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("true"))

            val fil1: InputStream = mockk()
            val fil2: InputStream = mockk()
            every { fil1.readAllBytes() } returns "test-fil".toByteArray()
            every { fil2.readAllBytes() } returns "div".toByteArray()

            val ettersendelsPdf = ByteArray(1)
            every { ettersendelsePdfGenerator.generate(any(), any()) } returns ettersendelsPdf
            coEvery { krypteringService.krypter(any(), any()) } returns fil1

            val files =
                listOf(
                    FilForOpplasting("filnavn0", "image/png", 1L, fil1),
                    FilForOpplasting("filnavn1", "image/jpg", 1L, fil2),
                )

            runCatching {
                fiksClient.lastOppNyEttersendelse(
                    files,
                    JsonVedleggSpesifikasjon(),
                    id,
                    "token",
                )
            }.let { assertThat(it.isSuccess) }
        }

    @Test
    internal fun `should produce body for upload`() {
        val fil1: InputStream = mockk()
        val fil2: InputStream = mockk()
        every { fil1.readAllBytes() } returns "test-fil".toByteArray()
        every { fil2.readAllBytes() } returns "div".toByteArray()

        val files =
            listOf(
                FilForOpplasting("filnavn0", "image/png", 1L, fil1),
                FilForOpplasting("filnavn1", "image/jpg", 1L, fil2),
            )
        val body = fiksClient.createBodyForUpload(JsonVedleggSpesifikasjon(), files)

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
