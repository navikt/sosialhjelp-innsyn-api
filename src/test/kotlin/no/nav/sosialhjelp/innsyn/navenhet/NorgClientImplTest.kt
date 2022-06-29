package no.nav.sosialhjelp.innsyn.navenhet

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.sosialhjelp.innsyn.client.tokendings.TokendingsService
import no.nav.sosialhjelp.innsyn.common.subjecthandler.StaticSubjectHandlerImpl
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.config.ClientProperties
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

internal class NorgClientImplTest {

    private val mockWebServer = MockWebServer()
    private val webClient: WebClient = WebClient.create(mockWebServer.url("/").toString())
    private val redisService: RedisService = mockk()
    private val clientProperties: ClientProperties = mockk()
    private val tokendingsService: TokendingsService = mockk()
    private val norgClient = NorgClientImpl(webClient, redisService, clientProperties, tokendingsService)

    private val enhetsnr = "8888"

    @BeforeEach
    fun init() {
        clearAllMocks()

        SubjectHandlerUtils.setNewSubjectHandlerImpl(StaticSubjectHandlerImpl())

        every { clientProperties.fssProxyAudience } returns "fssProxyAudience"

        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs
        coEvery { tokendingsService.exchangeToken(any(), any(), any()) } returns "tokenXtoken"
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    fun `skal hente fra cache`() {
        val navEnhet = objectMapper.readValue<NavEnhet>(ok_navenhet)
        every { redisService.get(any(), NavEnhet::class.java) } returns navEnhet

        val result2 = norgClient.hentNavEnhet(enhetsnr)

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisService.get(any(), any()) }
        verify(exactly = 0) { redisService.put(any(), any(), any()) }
    }

    @Test
    fun `skal hente fra Norg og lagre til cache hvis cache er tom`() {
        every { redisService.get(any(), NavEnhet::class.java) } returns null

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(ok_navenhet)
        )

        val result2 = norgClient.hentNavEnhet(enhetsnr)

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisService.get(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }
}
