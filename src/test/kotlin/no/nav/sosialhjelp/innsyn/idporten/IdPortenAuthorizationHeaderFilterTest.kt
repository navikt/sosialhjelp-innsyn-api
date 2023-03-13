package no.nav.sosialhjelp.innsyn.idporten

import io.mockk.every
import io.mockk.mockk
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

class IdPortenAuthorizationHeaderFilterTest {

    private val idPortenSessionHandler: IdPortenSessionHandler = mockk()
    private val idPortenAuthorizationHeaderFilter = IdPortenAuthorizationHeaderFilter(idPortenSessionHandler)

    private val token = "token"

    @Test
    fun `skal legge auth-header til request`() {
        every { idPortenSessionHandler.getToken(any()) } returns token

        val request = MockHttpServletRequest()

        val response = MockHttpServletResponse()
        val filter = AuthorizationHeaderCapturingMockFilterChain()

        idPortenAuthorizationHeaderFilter.doFilter(request, response, filter)

        assertThat(filter.capturedAuthorizationHeader()).isEqualTo(BEARER + token)
    }

    @Test
    fun `hvis sessionhandler ikke har token legges ikke auth-header til request`() {
        every { idPortenSessionHandler.getToken(any()) } returns null

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filter = AuthorizationHeaderCapturingMockFilterChain()

        idPortenAuthorizationHeaderFilter.doFilter(request, response, filter)

        assertThat(filter.capturedAuthorizationHeader()).isNull()
    }

    class AuthorizationHeaderCapturingMockFilterChain : FilterChain {
        private var authHeader: String? = null

        override fun doFilter(request: ServletRequest, response: ServletResponse) {
            val httpServletRequest = request as HttpServletRequest
            authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
        }

        fun capturedAuthorizationHeader() = authHeader
    }
}
