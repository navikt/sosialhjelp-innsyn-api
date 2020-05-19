package no.nav.sbl.sosialhjelpinnsynapi.utils.mdc

import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
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
        if (request.requestURI.startsWith(INNSYN_BASE_URL) || request.requestURI.startsWith(KLIENTLOGGER_BASE_URL) || request.requestURI.startsWith(VEIVISER_BASE_URL)) {
            addCallId(request)
            addDigisosId(request)
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            clearMDC()
        }
    }

    private fun addCallId(request: HttpServletRequest) {
        Optional.ofNullable(request.getHeader(HEADER_CALL_ID))
                .ifPresentOrElse(
                        {
                            setCallId(it)
                            log.info("Bruker call-id fra request: $it")
                        },
                        {
                            val callId = generateCallId()
                            setCallId(callId)
                            log.info("Genererte ny call-id: $callId")
                        }
                )
    }

    private fun addDigisosId(request: HttpServletRequest) {
        if (request.requestURI.matches(Regex("^${INNSYN_BASE_URL}(.*)/(forelopigSvar|hendelser|kommune|oppgaver|oppgaver/(.*)|orginalJsonSoknad|orginalSoknadPdlLink|saksStatus|soknadsStatus|vedlegg)"))) {
            val digisosId = request.requestURI.substringAfter(INNSYN_BASE_URL).substringBefore("/")
            setDigisosId(digisosId)
            log.info("Setter digisosId til mdc: $digisosId, requesturi: ${request.requestURI}")
        } else if (request.requestURI.matches(Regex("^${INNSYN_BASE_URL}saksDetaljer")) && request.parameterMap.containsKey("id")) {
            val digisosId = request.getParameter("id")
            setDigisosId(digisosId)
            log.info("Setter digisosId til mdc (saksDetaljer): $digisosId, requesturi: ${request.requestURI}")
        }
    }

    companion object {
        private const val CONTEXT_PATH = "/sosialhjelp/innsyn-api"
        private const val INNSYN_BASE_URL = "$CONTEXT_PATH/api/v1/innsyn/"
        private const val KLIENTLOGGER_BASE_URL = "$CONTEXT_PATH/api/v1/info/logg"
        private const val VEIVISER_BASE_URL = "$CONTEXT_PATH/api/veiviser/kommunenummer"

        private val log by logger()
    }

}