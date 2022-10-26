package no.nav.sosialhjelp.innsyn.kommunenummer

import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.sosialhjelp.innsyn.redis.KARTVERKET_KOMMUNENUMMER_KEY
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

internal class KommunenummerServiceTest {

    private val kartverketClient: KartverketClient = mockk()
    private val redisService: RedisService = mockk()

    private val kommunenummerService = KommunenummerService(kartverketClient, redisService)

    @Test
    internal fun `skal hente fra cache`() {
        every { redisService.get(KARTVERKET_KOMMUNENUMMER_KEY, any()) } returns "something"

        val response = kommunenummerService.getKommunenummer()

        assertThat(response).isEqualTo("something")

        verify { kartverketClient wasNot called }
    }

    @Test
    internal fun `skal hente fra server hvis cache er tom`() {
        every { redisService.get(KARTVERKET_KOMMUNENUMMER_KEY, any()) } returns null
        every { redisService.put(KARTVERKET_KOMMUNENUMMER_KEY, any(), any()) } just runs
        every { kartverketClient.getKommunenummer() } returns "something"

        val response = kommunenummerService.getKommunenummer()

        assertThat(response).isEqualTo("something")

        verify(exactly = 1) { redisService.put(KARTVERKET_KOMMUNENUMMER_KEY, any(), any()) }
    }

    @Test
    internal fun `skal kaste feil hvis cache er tom og server gir feil`() {
        every { redisService.get(KARTVERKET_KOMMUNENUMMER_KEY, any()) } returns null
        every { kartverketClient.getKommunenummer() } throws RuntimeException("some error")

        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy {
                kommunenummerService.getKommunenummer()
            }

        verify(exactly = 0) { redisService.put(KARTVERKET_KOMMUNENUMMER_KEY, any(), any()) }
    }
}
