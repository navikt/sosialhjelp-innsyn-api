package no.nav.sosialhjelp.innsyn.app.cors

import no.nav.sosialhjelp.innsyn.utils.MiljoUtils
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): FilterRegistrationBean<CorsFilter> {
        val source = UrlBasedCorsConfigurationSource()
        val bean = FilterRegistrationBean<CorsFilter>()
        val config = CorsConfiguration()
        config.allowedOrigins = if (MiljoUtils.isRunningInProd()) ALLOWED_ORIGINS_PROD else ALLOWED_ORIGINS_NON_PROD
        config.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "X-XSRF-TOKEN", "XSRF-TOKEN-INNSYN-API", "Authorization", "Nav-Call-Id")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowCredentials = true
        config.maxAge = 3600L
        source.registerCorsConfiguration("/**", config)
        val corsFilter = CorsFilter(source)
        bean.filter = corsFilter
        bean.order = Ordered.HIGHEST_PRECEDENCE
        return bean
    }

    companion object {
        private val ALLOWED_ORIGINS_PROD = listOf(
            "https://tjenester.nav.no",
            "https://www.nav.no"
        )

        private val ALLOWED_ORIGINS_NON_PROD = listOf("*")
    }
}
