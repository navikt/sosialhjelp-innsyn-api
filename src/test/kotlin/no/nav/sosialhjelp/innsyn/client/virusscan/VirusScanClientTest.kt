package no.nav.sosialhjelp.innsyn.client.virusscan

import io.mockk.every
import io.mockk.spyk
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.sosialhjelp.innsyn.common.OpplastingException
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

internal class VirusScanClientTest {

    private val mockWebServer = MockWebServer()
    private val webClient: WebClient = WebClient.create(mockWebServer.url("/").toString())

    private val virusScanClient = spyk(VirusScanClient(webClient))

    private val filnavn = "ikke-virustest"
    private val data = byteArrayOf()

    @BeforeEach
    fun setUp() {
        every { virusScanClient getProperty "enabled" } returns true
    }

    @Test
    fun scanFile_scanningIsNotEnabled_doesNotThrowException() {
        every { virusScanClient getProperty "enabled" } returns false
        assertThatCode { virusScanClient.scan(filnavn, data) }
            .doesNotThrowAnyException()
    }

    @Test
    fun scanFile_filenameIsVirustest_isInfected() {
        assertThatExceptionOfType(OpplastingException::class.java)
            .isThrownBy{ virusScanClient.scan("virustest", data) }
    }

    @Test
    fun scanFile_resultatHasWrongLength_isNotInfected() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(
                    listOf(ScanResult("test", Result.FOUND), ScanResult("test", Result.FOUND))
                ))
        )

        assertThatCode { virusScanClient.scan(filnavn, data) }
            .doesNotThrowAnyException()
    }

    @Test
    fun scanFile_resultatIsOK_isNotInfected() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(
                    listOf(ScanResult("test", Result.OK))
                ))
        )
        assertThatCode { virusScanClient.scan(filnavn, data) }
            .doesNotThrowAnyException()
    }

    @Test
    fun scanFile_resultatIsNotOK_isInfected() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(
                    listOf(ScanResult("test", Result.FOUND))
                ))
        )
        assertThatExceptionOfType(OpplastingException::class.java)
            .isThrownBy{ virusScanClient.scan(filnavn, data) }
    }
}