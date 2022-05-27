package no.nav.sosialhjelp.innsyn.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandler
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.config.XsrfGenerator.Companion.redisKey
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.redis.XSRF_KEY_PREFIX
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

internal class XsrfGeneratorTest {
    private val redisService: RedisService = mockk()
    private val request: HttpServletRequest = mockk()

    private val xsrfGenerator = XsrfGenerator(redisService)

    private val mockSubjectHandler: SubjectHandler = mockk()
    val token = "TokenX"

    @BeforeEach
    internal fun setUp() {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(mockSubjectHandler)

        every { mockSubjectHandler.getToken() } returns token
    }

    @Test
    fun generateXsrfToken_ok() {
        val idag = LocalDateTime.now()
        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs

        val generatedToken = xsrfGenerator.generateXsrfToken(token, idag)
        Assertions.assertThat(generatedToken).hasSize(43)
    }

    @Test
    fun generateXsrfToken_okOgForskjelligeHverGang() {
        val idag = LocalDateTime.now()
        every { redisService.get(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs

        val generatedToken1 = xsrfGenerator.generateXsrfToken(token, idag)
        Assertions.assertThat(generatedToken1).hasSize(43)
        val generatedToken2 = xsrfGenerator.generateXsrfToken(token, idag)
        Assertions.assertThat(generatedToken2).hasSize(43)
        Assertions.assertThat(generatedToken1).isNotEqualTo(generatedToken2)
    }

    @Test
    fun sjekkXsrfToken_ok_dagensToken() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns xsrfValue
        val keyIdag = redisKey(token, LocalDateTime.now())
        val keyIgar = redisKey(token, LocalDateTime.now().minusDays(1))
        every { redisService.get(XSRF_KEY_PREFIX + keyIdag, any()) } returns xsrfValue
        every { redisService.get(XSRF_KEY_PREFIX + keyIgar, any()) } returns null

        assertThatCode { xsrfGenerator.sjekkXsrfToken(request) }.doesNotThrowAnyException()
    }

    @Test
    fun sjekkXsrfToken_ok_garsdagensToken() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns xsrfValue
        val keyIdag = redisKey(token, LocalDateTime.now())
        val keyIgar = redisKey(token, LocalDateTime.now().minusDays(1))
        every { redisService.get(XSRF_KEY_PREFIX + keyIdag, any()) } returns null
        every { redisService.get(XSRF_KEY_PREFIX + keyIgar, any()) } returns xsrfValue

        assertThatCode { xsrfGenerator.sjekkXsrfToken(request) }.doesNotThrowAnyException()
    }

    @Test
    fun sjekkXsrfToken_error_ikkeFunnetIRedis() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns xsrfValue
        val keyIdag = redisKey(token, LocalDateTime.now())
        val keyIgar = redisKey(token, LocalDateTime.now().minusDays(1))
        every { redisService.get(XSRF_KEY_PREFIX + keyIdag, any()) } returns null
        every { redisService.get(XSRF_KEY_PREFIX + keyIgar, any()) } returns null

        assertThatThrownBy { xsrfGenerator.sjekkXsrfToken(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Feil xsrf token")
    }

    @Test
    fun sjekkXsrfToken_error_feilXsrfString() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns "feilXsrf"
        val keyIdag = redisKey(token, LocalDateTime.now())
        val keyIgar = redisKey(token, LocalDateTime.now().minusDays(1))
        every { redisService.get(XSRF_KEY_PREFIX + keyIdag, any()) } returns xsrfValue
        every { redisService.get(XSRF_KEY_PREFIX + keyIgar, any()) } returns null

        assertThatThrownBy { xsrfGenerator.sjekkXsrfToken(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Feil xsrf token")
    }
}
