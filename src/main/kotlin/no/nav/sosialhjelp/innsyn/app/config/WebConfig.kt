package no.nav.sosialhjelp.innsyn.app.config

import no.nav.sosialhjelp.innsyn.app.config.interceptor.TracingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tracingInterceptor: TracingInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tracingInterceptor)
    }
}
