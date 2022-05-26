package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.XSRF_TOKEN_INNSYN_API
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.XSRF_TOKEN_INNSYN_API_NY
import no.nav.sosialhjelp.innsyn.utils.MiljoUtils.isRunningInProd
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class CORSFilter : Filter {
    val log: Logger = LoggerFactory.getLogger(CORSFilter::class.java)

    @Throws(ServletException::class)
    override fun init(filterConfig: FilterConfig?) {
        // no-op
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val httpResponse = servletResponse as HttpServletResponse
        val origin = if (servletRequest is HttpServletRequest) (servletRequest.getHeader("Origin")) else null

        if (!isRunningInProd() && ALLOWED_ORIGINS.contains(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin)
            httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, X-XSRF-TOKEN, $XSRF_TOKEN_INNSYN_API, $XSRF_TOKEN_INNSYN_API_NY, Authorization, Nav-Call-Id")
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true")
        }
        filterChain.doFilter(servletRequest, httpResponse)
    }

    override fun destroy() {
        // no-op
    }

    companion object {
        private val ALLOWED_ORIGINS = listOf(
            "https://tjenester.nav.no",
            "https://www.nav.no"
        )
    }
}
