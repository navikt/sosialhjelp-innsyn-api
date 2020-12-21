package no.nav.sbl.sosialhjelpinnsynapi.client.norg

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.redis.RedisService
import no.nav.sbl.sosialhjelpinnsynapi.responses.ok_navenhet
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

internal class NorgClientImplTest {

    private val clientProperties: ClientProperties = mockk(relaxed = true)
    private val restTemplate: RestTemplate = mockk()
    private val redisService: RedisService = mockk()
    private val norgClient = NorgClientImpl(clientProperties, restTemplate, redisService)

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
        val mockResponse: ResponseEntity<String> = mockk()
        every { mockResponse.body } returns ok_navenhet
        every { redisService.get(any(), NavEnhet::class.java) } returns null
        every {
            restTemplate.exchange(
                    any(),
                    any(),
                    any(),
                    String::class.java,
                    enhetsnr)
        } returns mockResponse

        val result2 = norgClient.hentNavEnhet(enhetsnr)

        assertThat(result2).isNotNull

        verify(exactly = 1) { redisService.get(any(), any()) }
        verify(exactly = 1) { redisService.put(any(), any(), any()) }
    }
}