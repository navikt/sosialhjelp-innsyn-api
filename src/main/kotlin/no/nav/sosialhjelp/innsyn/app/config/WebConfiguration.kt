package no.nav.sosialhjelp.innsyn.app.config

import no.nav.sosialhjelp.innsyn.app.config.webfilter.TracingWebFilter
import no.nav.sosialhjelp.innsyn.app.config.webfilter.mdc.MDCFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader
import org.springframework.http.codec.multipart.MultipartHttpMessageReader
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.WebFilter

@Configuration
class WebConfiguration(
    private val tracingWebFilter: TracingWebFilter,
    private val mdcFilter: MDCFilter,
): WebFluxConfigurer {
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val partReader = DefaultPartHttpMessageReader()

        configurer.defaultCodecs().multipartReader(MultipartHttpMessageReader(partReader))
    }
    @Bean
    fun webFilters(): List<WebFilter> {
        return listOf(tracingWebFilter, mdcFilter)
    }
}
