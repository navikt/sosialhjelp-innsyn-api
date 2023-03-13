package no.nav.sosialhjelp.innsyn.idporten

import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.id.State
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.sosialhjelp.innsyn.app.exceptions.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.idporten.IdPortenController.Companion.LOGIN_ID_COOKIE
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import javax.servlet.http.Cookie

class IdPortenControllerTest {

    private val idPortenClient: IdPortenClient = mockk()
    private val idPortenProperties: IdPortenProperties = mockk(relaxed = true)
    private val redisService: RedisService = mockk()
    private val idPortenSessionHandler: IdPortenSessionHandler = mockk()

    private val idPortenController = IdPortenController(
        idPortenClient = idPortenClient,
        idPortenProperties = idPortenProperties,
        redisService = redisService,
        idPortenSessionHandler = idPortenSessionHandler
    )

    @Test
    fun `request mangler loginId-cookie - kaster TilgangskontrollException`() {
        val request = MockHttpServletRequest()
        request.requestURI = "www.redirect.com"

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy { idPortenController.handleCallback(request) }
    }

    @Test
    fun `mangler state i redis - kaster TilgangskontrollException`() {
        val request = MockHttpServletRequest()
        request.requestURI = "www.redirect.com"
        request.setCookies(Cookie(LOGIN_ID_COOKIE, "loginId"))

        every { redisService.get("${CachePrefixes.STATE_CACHE_PREFIX}loginId", State::class.java) } returns null

        assertThatExceptionOfType(TilgangskontrollException::class.java)
            .isThrownBy { idPortenController.handleCallback(request) }
    }

    @Test
    fun `state lagret i redis ulik state fra request - gir 403 Forbidden`() {
        val state = State()

        val request = MockHttpServletRequest()
        request.requestURI = "www.redirect.com"
        request.queryString = "state=state"
        request.setCookies(Cookie(LOGIN_ID_COOKIE, "loginId"))

        every { redisService.get("${CachePrefixes.STATE_CACHE_PREFIX}loginId", State::class.java) } returns state

        val response = idPortenController.handleCallback(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `happy path callback med redirect til innsyn`() {
        val state = State()
        val authCode = AuthorizationCode()

        val request = MockHttpServletRequest()
        request.requestURI = "www.redirect.com"
        request.queryString = "state=$state&code=$authCode"
        request.setCookies(Cookie(LOGIN_ID_COOKIE, "loginId"))

        every { redisService.get("${CachePrefixes.STATE_CACHE_PREFIX}loginId", State::class.java) } returns state

        every { idPortenClient.getToken(authCode, "loginId") } just runs
        every { idPortenSessionHandler.clearPropertiesForLogin("loginId") } just runs

        every { redisService.get("${CachePrefixes.LOGIN_REDIRECT_CACHE_PREFIX}loginId", String::class.java) } returns null

        val response = idPortenController.handleCallback(request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FOUND)
        assertThat(response.headers[HttpHeaders.LOCATION]).contains("/sosialhjelp/innsyn")
    }
}
