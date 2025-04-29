package no.nav.sosialhjelp.innsyn.app.cors

import no.nav.sosialhjelp.innsyn.app.MiljoUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig {
    @Bean
    fun addCorsConfig(): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry
                    .addMapping("/**")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedOriginPatterns(*allowedOrigins)
                    .allowedHeaders(
                        "Origin",
                        "Content-Type",
                        "Accept",
                        "Authorization",
                        "Nav-Call-Id",
                    ).allowCredentials(true)
                    .maxAge(3600L)
            }
        }

    companion object {
        private val allowedOrigins get() = if (MiljoUtils.isRunningInProd()) ALLOWED_ORIGINS_PROD else ALLOWED_ORIGINS_NON_PROD

        private val ALLOWED_ORIGINS_PROD =
            arrayOf(
                "https://tjenester.nav.no",
                "https://www.nav.no",
            )

        private val ALLOWED_ORIGINS_NON_PROD = arrayOf("*")
    }
}
