package no.nav.sbl.sosialhjelpinnsynapi.utils

import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.MDCUtils.clearCallId
import no.nav.sbl.sosialhjelpinnsynapi.utils.MDCUtils.setCallId

import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MDCFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        Optional.ofNullable(request.getHeader(HEADER_CALL_ID))
                .ifPresentOrElse(
                        { setCallId(it) },
                        { setCallId(generateCallId()) }
                )

        try {
            filterChain.doFilter(request, response)
        } finally {
            clearCallId()
        }
    }

}