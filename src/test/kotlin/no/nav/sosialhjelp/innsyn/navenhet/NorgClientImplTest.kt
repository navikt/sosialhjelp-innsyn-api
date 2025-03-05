package no.nav.sosialhjelp.innsyn.navenhet

import io.mockk.clearAllMocks
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import kotlin.time.Duration.Companion.seconds

internal class NorgClientImplTest {
    private val mockWebServer = MockWebServer()
    private val webClient: WebClient = WebClient.create(mockWebServer.url("/").toString())
    private val norgClient = NorgClientImpl(webClient)

    private val enhetsnr = "8888"

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    @AfterEach
    internal fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `skal trigge retry ved serverfeil fra Norg`() =
        runTest(timeout = 5.seconds) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500),
            )

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody(ok_navenhet),
            )

            val result2 = norgClient.hentNavEnhet(enhetsnr)

            assertThat(result2).isNotNull
        }
}
