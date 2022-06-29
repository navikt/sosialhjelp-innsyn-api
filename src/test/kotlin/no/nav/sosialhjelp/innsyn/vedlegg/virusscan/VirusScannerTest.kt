package no.nav.sosialhjelp.innsyn.vedlegg.virusscan

import no.nav.sosialhjelp.innsyn.common.VirusScanException
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

internal class VirusScannerTest {

    private val mockWebServer = MockWebServer()
    private val webClient: WebClient = WebClient.create(mockWebServer.url("/").toString())

    private lateinit var virusScanner: VirusScanner

    private val filnavn = "ikke-virustest"
    private val data = byteArrayOf()

    @AfterEach
    internal fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun scanFile_scanningIsNotEnabled_doesNotThrowException() {
        virusScanner = VirusScanner(webClient, enabled = false)
        assertThatCode { virusScanner.scan(filnavn, data) }
            .doesNotThrowAnyException()
    }

    @Test
    fun scanFile_filenameIsVirustest_isInfected() {
        virusScanner = VirusScanner(webClient, enabled = true)

        assertThatExceptionOfType(VirusScanException::class.java)
            .isThrownBy { virusScanner.scan("virustest", data) }
    }

    @Test
    fun scanFile_resultatHasWrongLength_isNotInfected() {
        virusScanner = VirusScanner(webClient, enabled = true)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(
                    objectMapper.writeValueAsString(
                        listOf(ScanResult("test", Result.FOUND), ScanResult("test", Result.FOUND))
                    )
                )
        )

        assertThatCode { virusScanner.scan(filnavn, data) }
            .doesNotThrowAnyException()
    }

    @Test
    fun scanFile_resultatIsOK_isNotInfected() {
        virusScanner = VirusScanner(webClient, enabled = true)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(
                    objectMapper.writeValueAsString(
                        listOf(ScanResult("test", Result.OK))
                    )
                )
        )
        assertThatCode { virusScanner.scan(filnavn, data) }
            .doesNotThrowAnyException()
    }

    @Test
    fun scanFile_resultatIsNotOK_isInfected() {
        virusScanner = VirusScanner(webClient, enabled = true)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(
                    objectMapper.writeValueAsString(
                        listOf(ScanResult("test", Result.FOUND))
                    )
                )
        )
        assertThatExceptionOfType(VirusScanException::class.java)
            .isThrownBy { virusScanner.scan(filnavn, data) }
    }
}
