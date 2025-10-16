package no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MDCUtils.DIGISOS_ID
import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MDCUtils.clearMDC
import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MDCUtils.put
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

@Component
class MDCFilter : CoWebFilter() {
    override suspend fun filter(
        exchange: ServerWebExchange,
        chain: CoWebFilterChain,
    ) {
        val request = exchange.request

        if (request.uri.path.contains(Regex("(/internal|/v3/api-docs)"))) {
            return chain.filter(exchange)
        }

        addDigisosId(request)
        put(MDCUtils.PATH, request.uri.path)
        request.headers.getFirst(HttpHeaders.USER_AGENT)?.let { put(MDCUtils.USER_AGENT, it) }
        request.headers.getFirst(HttpHeaders.REFERER)?.let { put(MDCUtils.REFERER, it) }
        // Fødselsdato blir ekskludert fra vanlig logging. Inkluderes altså kun i secure logs. Se logback-spring.xml
        TokenUtils.getUserIdFromTokenOrNull()?.take(6)?.let { put("fodselsdato", it) }
        withContext(MDCContext()) {
            chain.filter(exchange)
        }
        return clearMDC()
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
        } else if (path.matches(Regex("^$INNSYN_BASE_URL(.*)/detaljer"))) {
            val digisosId = path.substringAfter("${INNSYN_BASE_URL}sak/").substringBefore("/")
            put(DIGISOS_ID, digisosId)
        }
    }

    companion object {
        private const val INNSYN_BASE_URL = "/sosialhjelp/innsyn-api/api/v1/innsyn/"
    }
}
