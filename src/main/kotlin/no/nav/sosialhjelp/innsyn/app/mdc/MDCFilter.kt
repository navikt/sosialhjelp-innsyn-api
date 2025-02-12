package no.nav.sosialhjelp.innsyn.app.mdc

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.CALL_ID
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.DIGISOS_ID
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.clearMDC
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.generateCallId
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.put
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class MDCFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        addCallId(request)
        addDigisosId(request)
        put(MDCUtils.PATH, request.requestURI)
        request.getHeader(HttpHeaders.USER_AGENT)?.let { put(MDCUtils.USER_AGENT, it) }
        request.getHeader(HttpHeaders.REFERER)?.let { put(MDCUtils.REFERER, it) }
        SubjectHandlerUtils.getUserIdFromToken().take(6).let { put("fodselsdato", it) }

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
        if (request.requestURI.matches(
                Regex(
                    "^$INNSYN_BASE_URL(.*)/(forelopigSvar|hendelser|kommune|oppgaver|oppgaver/(.*)" +
                        "|saksStatus|soknadsStatus|vedlegg|vilkar|dokumentasjonkrav|dokumentasjonkrav/(.*)" +
                        "|harLeverteDokumentasjonkrav|fagsystemHarDokumentasjonkrav)",
                ),
            )
        ) {
            val digisosId = request.requestURI.substringAfter(INNSYN_BASE_URL).substringBefore("/")
            put(DIGISOS_ID, digisosId)
        } else if (request.requestURI.matches(Regex("^${INNSYN_BASE_URL}(.*)/detaljer"))
        ) {
            val digisosId = request.requestURI.substringAfter("${INNSYN_BASE_URL}sak/").substringBefore("/")
            put(DIGISOS_ID, digisosId)
        }
    }

    companion object {
        private const val INNSYN_BASE_URL = "/sosialhjelp/innsyn-api/api/v1/innsyn/"
    }
}
