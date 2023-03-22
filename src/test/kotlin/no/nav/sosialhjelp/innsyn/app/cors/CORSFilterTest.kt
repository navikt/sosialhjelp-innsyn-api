package no.nav.sosialhjelp.innsyn.app.cors

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import jakarta.servlet.http.HttpServletRequest
import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

internal class CORSFilterTest {

    private val corsFilter = CORSFilter()

    private val filterChain = MockFilterChain()

    private val unknownOrigin = "www.unknown.com"
    private val trustedOrigin = "https://www.nav.no"

    @BeforeEach
    internal fun setUp() {
        mockkObject(MiljoUtils)
        every { MiljoUtils.isRunningInProd() } returns true
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(MiljoUtils)
    }

    @Test
    internal fun `unknown origin should not set cors headers`() {
        val request: HttpServletRequest = mockk()
        every { request.requestURI } returns "requestURI"
        every { request.getHeader("Origin") } returns unknownOrigin

        val response = MockHttpServletResponse()

        corsFilter.doFilter(request, response, filterChain)

        assertThat(response.headerNames).isEmpty()
    }

    @Disabled("Lokal og test kjøremiljø sender inn CORS verdi før api-kall går gjennom doFilter(), burde ikke sette ekstra CORS headere")
    @Test
    internal fun `trusted origin should set cors headers`() {
        val request = MockHttpServletRequest()
        request.requestURI = "requestURI"
        request.addHeader("origin", trustedOrigin)

        val response = MockHttpServletResponse()

        corsFilter.doFilter(request, response, filterChain)

        assertThat(response.headerNames).contains("Access-Control-Allow-Origin")
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(trustedOrigin)
        assertThat(response.headerNames).contains("Access-Control-Allow-Headers")
        assertThat(response.headerNames).contains("Access-Control-Allow-Methods")
        assertThat(response.headerNames).contains("Access-Control-Allow-Credentials")
    }

    @Disabled("Lokal og test kjøremiljø sender inn CORS verdi før api-kall går gjennom doFilter(), burde ikke sette ekstra CORS headere")
    @Test
    internal fun `should set cors headers when non-prod`() {
        every { MiljoUtils.isRunningInProd() } returns false

        val request = MockHttpServletRequest()
        request.requestURI = "requestURI"
        request.addHeader("origin", unknownOrigin)

        val response = MockHttpServletResponse()

        corsFilter.doFilter(request, response, filterChain)

        assertThat(response.headerNames).contains("Access-Control-Allow-Origin")
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(unknownOrigin)
        assertThat(response.headerNames).contains("Access-Control-Allow-Headers")
        assertThat(response.headerNames).contains("Access-Control-Allow-Methods")
        assertThat(response.headerNames).contains("Access-Control-Allow-Credentials")
    }
}
