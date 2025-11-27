package no.nav.sosialhjelp.innsyn.app.config.webfilter

import io.opentelemetry.api.trace.Span
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.URI

interface TracingWebFilter : WebFilter

@Component
@Profile("!mock-alt")
class DefaultTracingWebFilter : TracingWebFilter {
    private val log by logger()

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val request = exchange.request
        if (request.uri.path.contains(Regex("(/internal|/v3/api-docs)"))) {
            return chain.filter(exchange)
        }

        val fiksDigisosId = request.uri.extractFiksDigisosId()

        if (fiksDigisosId != null) {
            val currentSpan = Span.current()
            if (!currentSpan.spanContext.isValid) {
                log.warn("Invalid span context")
            }
            currentSpan.setAttribute("fiksdigisosid", fiksDigisosId)
        }

        return chain.filter(exchange)
    }

    private fun URI.extractFiksDigisosId(): String? {
        val uuidRegex = Regex("""[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""")
        return uuidRegex.find(path)?.value
    }
}

@Component
@Profile("mock-alt")
class LocalTracingWebFilter : TracingWebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> = chain.filter(exchange)
}
