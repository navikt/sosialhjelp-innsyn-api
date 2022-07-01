package no.nav.sosialhjelp.innsyn.app.cors

import no.nav.sosialhjelp.innsyn.utils.MiljoUtils.isRunningInProd
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class CORSFilter : Filter {

    override fun init(filterConfig: FilterConfig?) {
        // no-op
    }

    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val httpResponse = servletResponse as HttpServletResponse
        val origin = if (servletRequest is HttpServletRequest) (servletRequest.getHeader("Origin")) else null

        if (!isRunningInProd() || ALLOWED_ORIGINS.contains(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin)
            httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, X-XSRF-TOKEN, XSRF-TOKEN-INNSYN-API, Authorization, Nav-Call-Id")
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
