package no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MDCUtils.DIGISOS_ID
import org.springframework.http.HttpHeaders
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

        val mdcValues =
            buildMap {
                DIGISOS_ID_PATH_REGEX.find(request.uri.path)?.groupValues?.get(1)?.let { put(DIGISOS_ID, it) }
                put(MDCUtils.PATH, request.uri.path)
                put(MDCUtils.HTTP_METHOD, request.method.name())
                request.headers.getFirst(HttpHeaders.USER_AGENT)?.let { put(MDCUtils.USER_AGENT, it) }
                request.headers.getFirst(HttpHeaders.REFERER)?.let { put(MDCUtils.REFERER, it) }
            }

        withContext(MDCContext(mdcValues)) {
            chain.filter(exchange)
        }
    }

    companion object {
        private const val UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        private val DIGISOS_ID_PATH_REGEX =
            Regex("(?:/sosialhjelp/innsyn-api)?/api/v[12]/innsyn/(?:sak/)?($UUID_PATTERN)(?:/|$)")
    }
}
