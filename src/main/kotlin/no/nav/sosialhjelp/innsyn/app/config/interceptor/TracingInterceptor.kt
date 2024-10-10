package no.nav.sosialhjelp.innsyn.app.config.interceptor

import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sosialhjelp.innsyn.app.getFiksDigisosId
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

interface TracingInterceptor : HandlerInterceptor

@Component
@Profile("!mock-alt")
class DefaultTracingInterceptor : TracingInterceptor {
    private val log by logger()

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val fiksDigisosId = request.getFiksDigisosId()

        if (fiksDigisosId != null) {
            val currentSpan = Span.current()
            if (!currentSpan.spanContext.isValid) {
                log.warn("Invalid span context")
            }
            currentSpan.setAttribute("fiksdigisosid", fiksDigisosId)
        }

        return true
    }
}

@Component
@Profile("mock-alt")
class LocalTracingInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean = true
}
