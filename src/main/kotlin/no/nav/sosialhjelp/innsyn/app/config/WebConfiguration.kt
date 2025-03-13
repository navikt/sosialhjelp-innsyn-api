package no.nav.sosialhjelp.innsyn.app.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import no.nav.sosialhjelp.innsyn.app.config.interceptor.TracingInterceptor
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tracingInterceptor: TracingInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tracingInterceptor)
    }

    @Bean
    fun trailingSlashRedirectFilter(): Filter = TrailingSlashRedirectFilter()

    @Bean
    fun trailingSlashFilter(): FilterRegistrationBean<Filter> {
        val registrationBean = FilterRegistrationBean<Filter>()
        registrationBean.filter = trailingSlashRedirectFilter()
        registrationBean.addUrlPatterns("/*")
        return registrationBean
    }
}

class TrailingSlashRedirectFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse?,
        chain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        val path = httpRequest.requestURI
        if (path.endsWith("/")) {
            val newPath = path.substring(0, path.length - 1)
            val newRequest: HttpServletRequest = CustomHttpServletRequestWrapper(httpRequest, newPath)
            chain.doFilter(newRequest, response)
        } else {
            chain.doFilter(request, response)
        }
    }
}

private class CustomHttpServletRequestWrapper(
    request: HttpServletRequest?,
    private val newPath: String,
) : HttpServletRequestWrapper(
        request,
    ) {
    override fun getRequestURI(): String = newPath

    override fun getRequestURL(): StringBuffer {
        val url = StringBuffer()
        url
            .append(scheme)
            .append("://")
            .append(serverName)
            .append(":")
            .append(serverPort)
            .append(newPath)
        return url
    }
}
