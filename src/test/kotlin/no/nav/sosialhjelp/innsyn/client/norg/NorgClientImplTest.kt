package no.nav.sosialhjelp.innsyn.client.norg

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sosialhjelp.innsyn.domain.NavEnhet
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.responses.ok_navenhet
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.function.Consumer

internal class NorgClientImplTest {

    private val webClient: WebClient = mockk()
    private val redisService: RedisService = mockk()
    private val norgClient = NorgClientImpl(webClient, redisService)

    private val enhetsnr = "8888"

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs
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
        val navEnhet = objectMapper.readValue<NavEnhet>(ok_navenhet)
        every { redisService.get(any(), NavEnhet::class.java) } returns null
        every {
            webClient
                .get()
                .uri("/enhet/{enhetsnr}", enhetsnr)
                .headers(any<Consumer<HttpHeaders>>())
                .retrieve()
                .bodyToMono<NavEnhet>()
                .block()
        } returns navEnhet

        val result2 = norgClient.hentNavEnhet(enhetsnr)

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisService.get(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }
}