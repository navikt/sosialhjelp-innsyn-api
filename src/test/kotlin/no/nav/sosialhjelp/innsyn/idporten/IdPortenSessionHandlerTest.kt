package no.nav.sosialhjelp.innsyn.idporten

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sosialhjelp.innsyn.idporten.IdPortenController.Companion.LOGIN_ID_COOKIE
import no.nav.sosialhjelp.innsyn.redis.RedisService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import javax.servlet.http.Cookie

internal class IdPortenSessionHandlerTest {

    private val redisService: RedisService = mockk()
    private val idPortenClient: IdPortenClient = mockk()

    private val idPortenSessionHandler = IdPortenSessionHandler(redisService, idPortenClient)

    private val request = MockHttpServletRequest()

    @BeforeEach
    internal fun setUp() {
        request.setCookies(Cookie(LOGIN_ID_COOKIE, "uuid"))
    }

    @Test
    internal fun `skal hente accesstoken fra redis`() {
        every { redisService.get("IDPORTEN_ACCESS_TOKEN_uuid", String::class.java) } returns "token"

        assertThat(idPortenSessionHandler.getToken(request)).isEqualTo("token")

        verify(exactly = 1) { redisService.get(any(), String::class.java) }
        verify { idPortenClient wasNot called }
    }

    @Test
    internal fun `skal hente accessToken fra refreshToken`() {
        every { redisService.get("IDPORTEN_ACCESS_TOKEN_uuid", String::class.java) } returns null
        every { redisService.get("IDPORTEN_REFRESH_TOKEN_uuid", String::class.java) } returns "refreshtoken"
        every { idPortenClient.getAccessTokenFromRefreshToken("refreshtoken", "uuid") } returns "token"

        assertThat(idPortenSessionHandler.getToken(request)).isEqualTo("token")

        verify(exactly = 2) { redisService.get(any(), String::class.java) }
        verify(exactly = 1) { idPortenClient.getAccessTokenFromRefreshToken(any(), any()) }
    }

    @Test
    internal fun `skal returnere null hvis verken accessToken eller refreshToken er cachet`() {
        every { redisService.get("IDPORTEN_ACCESS_TOKEN_uuid", String::class.java) } returns null
        every { redisService.get("IDPORTEN_REFRESH_TOKEN_uuid", String::class.java) } returns null

        assertThat(idPortenSessionHandler.getToken(request)).isNull()

        verify(exactly = 2) { redisService.get(any(), String::class.java) }
        verify { idPortenClient wasNot called }
    }

    @Test
    internal fun `skal returnere null hvis login_id ikke finnes i cookies`() {
        val requestWithoutCookie = MockHttpServletRequest()

        assertThat(idPortenSessionHandler.getToken(requestWithoutCookie)).isNull()

        verify { redisService wasNot called }
        verify { idPortenClient wasNot called }
    }
}
