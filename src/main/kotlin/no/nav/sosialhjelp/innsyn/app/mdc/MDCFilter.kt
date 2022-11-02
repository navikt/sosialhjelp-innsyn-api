package no.nav.sosialhjelp.innsyn.app.mdc

import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.CALL_ID
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.DIGISOS_ID
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.clearMDC
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.generateCallId
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.put
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class MDCFilter : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        addCallId(request)
        addDigisosId(request)
        put(MDCUtils.PATH, request.requestURI)
        request.getHeader(HttpHeaders.USER_AGENT)?.let { put(MDCUtils.USER_AGENT, it) }
        request.getHeader(HttpHeaders.REFERER)?.let { put(MDCUtils.REFERER, it) }

        try {
            filterChain.doFilter(request, response)
        } finally {
            clearMDC()
        }
    }

    private fun addCallId(request: HttpServletRequest) {
        request.getHeader(HEADER_CALL_ID)
            ?.let { put(CALL_ID, it) }
            ?: put(CALL_ID, generateCallId())
    }

    private fun addDigisosId(request: HttpServletRequest) {
        if (request.requestURI.matches(Regex("^$INNSYN_BASE_URL(.*)/(forelopigSvar|hendelser|kommune|oppgaver|oppgaver/(.*)|orginalJsonSoknad|orginalSoknadPdlLink|saksStatus|soknadsStatus|vedlegg)"))) {
            val digisosId = request.requestURI.substringAfter(INNSYN_BASE_URL).substringBefore("/")
            put(DIGISOS_ID, digisosId)
        } else if (request.requestURI.matches(Regex("^${INNSYN_BASE_URL}saksDetaljer")) && request.parameterMap.containsKey("id")) {
            val digisosId = request.getParameter("id")
            put(DIGISOS_ID, digisosId)
        }
    }

    companion object {
        private const val INNSYN_BASE_URL = "/sosialhjelp/innsyn-api/api/v1/innsyn/"
    }
}
