package no.nav.sosialhjelp.innsyn.app.xsrf

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import no.nav.sosialhjelp.innsyn.app.exceptions.XsrfException
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandler
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.app.xsrf.XsrfGenerator.Companion.redisKey
import no.nav.sosialhjelp.innsyn.redis.RedisService
import no.nav.sosialhjelp.innsyn.redis.XSRF_KEY_PREFIX
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class XsrfGeneratorTest {
    private val redisService: RedisService = mockk()
    private val request: HttpServletRequest = mockk()

    private val xsrfGenerator = XsrfGenerator(redisService)

    private val mockSubjectHandler: SubjectHandler = mockk()
    private val fnr = "fnr"

    @BeforeEach
    internal fun setUp() {
        SubjectHandlerUtils.setNewSubjectHandlerImpl(mockSubjectHandler)
        every { mockSubjectHandler.getUserIdFromToken() } returns fnr
    }

    @AfterEach
    internal fun tearDown() {
        SubjectHandlerUtils.resetSubjectHandlerImpl()
    }

    @Test
    fun generateXsrfToken_ok() {
        val idag = LocalDateTime.now()
        every { redisService.get<Any>(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs

        val generatedToken = xsrfGenerator.generateXsrfToken(idag)
        assertThat(generatedToken).hasSize(44)
    }

    @Test
    fun generateXsrfToken_okOgForskjelligeHverGang() {
        val idag = LocalDateTime.now()
        every { redisService.get<Any>(any(), any()) } returns null
        every { redisService.put(any(), any(), any()) } just Runs

        val generatedToken1 = xsrfGenerator.generateXsrfToken(idag)
        assertThat(generatedToken1).hasSize(44)
        val generatedToken2 = xsrfGenerator.generateXsrfToken(idag)
        assertThat(generatedToken2).hasSize(44)
        assertThat(generatedToken1).isNotEqualTo(generatedToken2)
    }

    @Test
    fun sjekkXsrfToken_ok_dagensToken() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns xsrfValue
        val keyIdag = redisKey(fnr, LocalDateTime.now())
        val keyIgar = redisKey(fnr, LocalDateTime.now().minusDays(1))
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIdag, any()) } returns xsrfValue
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIgar, any()) } returns null

        assertThatCode { xsrfGenerator.sjekkXsrfToken(request) }.doesNotThrowAnyException()
    }

    @Test
    fun sjekkXsrfToken_ok_garsdagensToken() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns xsrfValue
        val keyIdag = redisKey(fnr, LocalDateTime.now())
        val keyIgar = redisKey(fnr, LocalDateTime.now().minusDays(1))
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIdag, any()) } returns null
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIgar, any()) } returns xsrfValue

        assertThatCode { xsrfGenerator.sjekkXsrfToken(request) }.doesNotThrowAnyException()
    }

    @Test
    fun sjekkXsrfToken_error_ikkeFunnetIRedis() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns xsrfValue
        val keyIdag = redisKey(fnr, LocalDateTime.now())
        val keyIgar = redisKey(fnr, LocalDateTime.now().minusDays(1))
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIdag, any()) } returns null
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIgar, any()) } returns null

        assertThatThrownBy { xsrfGenerator.sjekkXsrfToken(request) }
            .isInstanceOf(XsrfException::class.java)
            .hasMessage("Feil xsrf token")
    }

    @Test
    fun sjekkXsrfToken_error_feilXsrfString() {
        val xsrfValue = "XSRF"
        every { request.getHeader("XSRF-TOKEN-INNSYN-API") } returns "feilXsrf"
        val keyIdag = redisKey(fnr, LocalDateTime.now())
        val keyIgar = redisKey(fnr, LocalDateTime.now().minusDays(1))
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIdag, any()) } returns xsrfValue
        every { redisService.get<String>(XSRF_KEY_PREFIX + keyIgar, any()) } returns null

        assertThatThrownBy { xsrfGenerator.sjekkXsrfToken(request) }
            .isInstanceOf(XsrfException::class.java)
            .hasMessage("Feil xsrf token")
    }
}
