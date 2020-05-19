package no.nav.sbl.sosialhjelpinnsynapi.utils.mdc

import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils.clearMDC
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils.generateCallId
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils.setCallId
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils.setDigisosId

import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MDCFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        addCallId(request)
        addDigisosId(request)

        try {
            filterChain.doFilter(request, response)
        } finally {
            clearMDC()
        }
    }

    private fun addCallId(request: HttpServletRequest) {
        Optional.ofNullable(request.getHeader(HEADER_CALL_ID))
                .ifPresentOrElse(
                        { setCallId(it) },
                        { setCallId(generateCallId()) }
                )
    }

    private fun addDigisosId(request: HttpServletRequest) {
        if (request.requestURI.matches(Regex("^${INNSYN_BASE_URL}(.*)/(forelopigSvar|hendelser|kommune|oppgaver|oppgaver/(.*)|orginalJsonSoknad|orginalSoknadPdlLink|saksStatus|soknadsStatus|vedlegg)"))) {
            val digisosId = request.requestURI.substringAfter(INNSYN_BASE_URL).substringBefore("/")
            setDigisosId(digisosId)
        } else if (request.requestURI.matches(Regex("^${INNSYN_BASE_URL}saksDetaljer")) && request.parameterMap.containsKey("id")) {
            val digisosId = request.getParameter("id")
            setDigisosId(digisosId)
        }
    }

    companion object {
        private const val INNSYN_BASE_URL = "/sosialhjelp/innsyn-api/api/v1/innsyn/"
    }

}