package no.nav.sosialhjelp.innsyn.app.config.webfilter

import io.opentelemetry.api.trace.Span
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

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
        if (request.uri.path.contains("/internal")) {
            return chain.filter(exchange)
        }
        val fiksDigisosId = exchange.getAttribute<String>("fiksDigisosId")

        if (fiksDigisosId != null) {
            val currentSpan = Span.current()
            if (!currentSpan.spanContext.isValid) {
                log.warn("Invalid span context")
            }
            currentSpan.setAttribute("fiksdigisosid", fiksDigisosId)
        }

        return chain.filter(exchange)
    }
}

@Component
@Profile("mock-alt")
class LocalTracingWebFilter : TracingWebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        return chain.filter(exchange)
    }
}
