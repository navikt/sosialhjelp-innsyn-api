package no.nav.sosialhjelp.innsyn.app.config

import no.nav.sosialhjelp.innsyn.app.config.webfilter.AuthWebFilter
import no.nav.sosialhjelp.innsyn.app.config.webfilter.TracingWebFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.WebFilter

@Configuration
class WebConfiguration(
    private val tracingWebFilter: TracingWebFilter,
    private val authWebFilter: AuthWebFilter,
) {
    @Bean
    fun webFilters(): List<WebFilter> {
        return listOf(authWebFilter, tracingWebFilter)
    }
}
