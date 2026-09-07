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
        clearMDC()

        if (request.uri.path.contains(Regex("(/internal|/v3/api-docs)"))) {
            return chain.filter(exchange)
        }

        addDigisosId(request)
        put(MDCUtils.PATH, request.uri.path)
        put(MDCUtils.HTTP_METHOD, request.method.name())

        request.headers.getFirst(HttpHeaders.USER_AGENT)?.let { put(MDCUtils.USER_AGENT, it) }
        request.headers.getFirst(HttpHeaders.REFERER)?.let { put(MDCUtils.REFERER, it) }

        // Fødselsdato blir ekskludert fra vanlig logging. Inkluderes altså kun i secure logs. Se logback-spring.xml
        TokenUtils.getUserIdFromTokenOrNull()?.take(6)?.let { put("fodselsdato", it) }

        try {
            withContext(MDCContext()) {
                chain.filter(exchange)
            }
        } finally {
            clearMDC()
        }
    }

    private fun addDigisosId(request: ServerHttpRequest) {
        val digisosId = DIGISOS_ID_PATH_REGEX.find(request.uri.path)?.groupValues?.get(1)
        if (digisosId != null) {
            put(DIGISOS_ID, digisosId)
        }
    }

    companion object {
        private const val UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        private val DIGISOS_ID_PATH_REGEX =
            Regex("(?:/sosialhjelp/innsyn-api)?/api/v[12]/innsyn/(?:sak/)?($UUID_PATTERN)(?:/|$)")
    }
}
