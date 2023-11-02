import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebConfig {
    @Bean
    fun trailingSlashRedirectFilter(): Filter {
        return TrailingSlashRedirectFilter()
    }

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

private class CustomHttpServletRequestWrapper(request: HttpServletRequest?, private val newPath: String) : HttpServletRequestWrapper(
    request,
) {
    override fun getRequestURI(): String {
        return newPath
    }

    override fun getRequestURL(): StringBuffer {
        val url = StringBuffer()
        url.append(scheme).append("://").append(serverName).append(":").append(serverPort)
            .append(newPath)
        return url
    }
}
