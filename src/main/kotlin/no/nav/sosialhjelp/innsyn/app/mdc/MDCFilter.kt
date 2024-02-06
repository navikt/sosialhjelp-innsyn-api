package no.nav.sosialhjelp.innsyn.app.mdc

import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.CALL_ID
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.DIGISOS_ID
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.clearMDC
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.generateCallId
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils.put
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

@Component
class MDCFilter : CoWebFilter() {
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val request = exchange.request
        addCallId(request)
        addDigisosId(request)
        put(MDCUtils.PATH, request.uri.path)
        request.headers[HttpHeaders.USER_AGENT]?.let { put(MDCUtils.USER_AGENT, it.joinToString()) }
        request.headers[HttpHeaders.REFERER]?.let { put(MDCUtils.REFERER, it.joinToString()) }
        try {
            chain.filter(exchange)
        } finally {
            clearMDC()
        }
    }

    private fun addCallId(request: ServerHttpRequest) {
        request.headers[HEADER_CALL_ID]
            ?.let { put(CALL_ID, it.joinToString()) }
            ?: put(CALL_ID, generateCallId())
    }

    private fun addDigisosId(request: ServerHttpRequest) {
        val path = request.uri.path
        if (path.matches(
                Regex(
                    "^$INNSYN_BASE_URL(.*)/(forelopigSvar|hendelser|kommune|oppgaver|oppgaver/(.*)" +
                        "|saksStatus|soknadsStatus|vedlegg|vilkar|dokumentasjonkrav|dokumentasjonkrav/(.*)" +
                        "|harLeverteDokumentasjonkrav|fagsystemHarDokumentasjonkrav)",
                ),
            )
        ) {
            val digisosId = path.substringAfter(INNSYN_BASE_URL).substringBefore("/")
            put(DIGISOS_ID, digisosId)
        } else if (path.matches(Regex("^${INNSYN_BASE_URL}saksDetaljer")) &&
            request.queryParams.containsKey(
                "id",
            )
        ) {
            val digisosId = request.queryParams["id"]
            put(DIGISOS_ID, digisosId!!.first())
        }
    }

    companion object {
        private const val INNSYN_BASE_URL = "/sosialhjelp/innsyn-api/api/v1/innsyn/"
    }
}
