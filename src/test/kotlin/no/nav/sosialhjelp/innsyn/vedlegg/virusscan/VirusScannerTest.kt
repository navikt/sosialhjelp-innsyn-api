package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import kotlinx.coroutines.test.runTest
import no.nav.sosialhjelp.innsyn.app.exceptions.VirusScanException
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.reactive.function.client.WebClient
import kotlin.time.Duration.Companion.seconds

internal class VirusScannerTest {
    private val mockWebServer = MockWebServer()
    private val webClient: WebClient = WebClient.create(mockWebServer.url("/").toString())

    private lateinit var virusScanner: VirusScanner

    private val filnavn = "ikke-virustest"
    private val data = MockMultipartFile("test", "rofl", "application/json", byteArrayOf())

    @AfterEach
    internal fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun scanFile_scanningIsNotEnabled_doesNotThrowException() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = false)
            assertThat(runCatching { virusScanner.scan(filnavn, data) }.isSuccess)
        }

    @Test
    fun scanFile_filenameIsVirustest_isInfected() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = true)

            runCatching { virusScanner.scan("virustest", data) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(VirusScanException::class.java)
            }
        }

    @Test
    fun scanFile_resultatHasWrongLength_isNotInfected() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(
                        objectMapper.writeValueAsString(
                            listOf(ScanResult("test", Result.FOUND), ScanResult("test", Result.FOUND)),
                        ),
                    ),
            )

            assertThat(kotlin.runCatching { virusScanner.scan(filnavn, data) }.isSuccess)
        }

    @Test
    fun scanFile_resultatIsOK_isNotInfected() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(
                        objectMapper.writeValueAsString(
                            listOf(ScanResult("test", Result.OK)),
                        ),
                    ),
            )
            assertThat(kotlin.runCatching { virusScanner.scan(filnavn, data) }.isSuccess)
        }

    @Test
    fun scanFile_resultatIsNotOK_isInfected() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(
                        objectMapper.writeValueAsString(
                            listOf(ScanResult("test", Result.FOUND)),
                        ),
                    ),
            )
            runCatching { virusScanner.scan(filnavn, data) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(VirusScanException::class.java)
            }
        }

    @Test
    fun scanFile_resultatIsError_isInfected() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(
                        objectMapper.writeValueAsString(
                            listOf(ScanResult("test", Result.ERROR)),
                        ),
                    ),
            )
            kotlin.runCatching { virusScanner.scan(filnavn, data) }.let {
                assertThat(it.isFailure)
                assertThat(it.exceptionOrNull()).isInstanceOf(VirusScanException::class.java)
            }
        }

    @Test
    fun `skal trigge retry ved serverfeil`() =
        runTest(timeout = 5.seconds) {
            virusScanner = VirusScanner(webClient, enabled = true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500),
            )

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(
                        objectMapper.writeValueAsString(
                            listOf(ScanResult("test", Result.OK)),
                        ),
                    ),
            )

            assertThat(kotlin.runCatching { virusScanner.scan(filnavn, data) }.isSuccess)
        }
}
