package no.nav.sosialhjelp.innsyn.idporten

import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.Enumeration
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

/**
 * Beriker innkommende request med Authorization header, hvis vi har data for login_id i Redis
 */
@Profile("idporten")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class IdPortenAuthorizationHeaderFilter(
    private val idPortenSessionHandler: IdPortenSessionHandler
) : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest) {
            val accessToken = idPortenSessionHandler.getToken(request)
            if (accessToken != null) {
                val mutableRequest = MutableHttpServletRequest(request)
                mutableRequest.putHeader(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
                chain.doFilter(mutableRequest, response)
                return
            }
        }
        chain.doFilter(request, response)
    }

    private class MutableHttpServletRequest(
        request: HttpServletRequest,
    ) : HttpServletRequestWrapper(request) {

        private val customHeaders: MutableMap<String, String>

        init {
            customHeaders = hashMapOf()
        }

        fun putHeader(name: String, value: String) {
            customHeaders[name] = value
        }

        override fun getHeader(name: String?): String? {
            // check the custom headers first
            val headerValue = customHeaders[name]
            // else return from into the original wrapped object
            return headerValue ?: (request as HttpServletRequest).getHeader(name)
        }

        override fun getHeaderNames(): Enumeration<String>? {
            // create a set of the custom header names
            val set: MutableSet<String> = HashSet(customHeaders.keys)

            // now add the headers from the wrapped request object
            val e: Enumeration<String> = (request as HttpServletRequest).headerNames
            while (e.hasMoreElements()) {
                // add the names of the request headers into the list
                val n: String = e.nextElement()
                set.add(n)
            }

            // create an enumeration from the set and return
            return Collections.enumeration(set)
        }

        override fun getHeaders(name: String?): Enumeration<String> {
            // check the custom headers first
            val headerValue = customHeaders[name]
            // else return from into the original wrapped object
            return headerValue?.let { Collections.enumeration(setOf(it)) } ?: super.getHeaders(name)
        }
    }
}
