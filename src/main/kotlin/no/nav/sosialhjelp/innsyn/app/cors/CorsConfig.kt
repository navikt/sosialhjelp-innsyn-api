package no.nav.sosialhjelp.innsyn.app.cors

import no.nav.sosialhjelp.innsyn.utils.MiljoUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig {

    @Bean
    fun addCorsConfig(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                val allowedOrigins =
                    if (MiljoUtils.isRunningInProd()) ALLOWED_ORIGINS_PROD else ALLOWED_ORIGINS_NON_PROD
                registry.addMapping("/**")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedOriginPatterns(*allowedOrigins)
                    .allowedHeaders(
                        "Origin",
                        "Content-Type",
                        "Accept",
                        "X-XSRF-TOKEN",
                        "XSRF-TOKEN-INNSYN-API",
                        "Authorization",
                        "Nav-Call-Id"
                    )
                    .allowCredentials(true)
                    .maxAge(3600L)
            }
        }
    }

//    @Bean
//    fun corsFilter(): FilterRegistrationBean<CorsFilter> {
//        val source = UrlBasedCorsConfigurationSource()
//        val bean = FilterRegistrationBean<CorsFilter>()
//        val config = CorsConfiguration()
//        config.allowedOrigins = if (MiljoUtils.isRunningInProd()) ALLOWED_ORIGINS_PROD else ALLOWED_ORIGINS_NON_PROD
//        config.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "X-XSRF-TOKEN", "XSRF-TOKEN-INNSYN-API", "Authorization", "Nav-Call-Id")
//        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
//        config.allowCredentials = true
//        config.maxAge = 3600L
//        source.registerCorsConfiguration("/**", config)
//        val corsFilter = CorsFilter(source)
//        bean.filter = corsFilter
//        bean.order = Ordered.HIGHEST_PRECEDENCE
//        return bean
//    }

    // val httpResponse = servletResponse as HttpServletResponse
//    val origin = if (servletRequest is HttpServletRequest) (servletRequest.getHeader("Origin")) else null
//
//    if (!isRunningInProd() || ALLOWED_ORIGINS.contains(origin)) {
//        httpResponse.setHeader("Access-Control-Allow-Origin", origin)
//        httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, X-XSRF-TOKEN, XSRF-TOKEN-INNSYN-API, Authorization, Nav-Call-Id")
//        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
//        httpResponse.setHeader("Access-Control-Allow-Credentials", "true")
//    }
//    filterChain.doFilter(servletRequest, httpResponse)
// }

    companion object {
        private val ALLOWED_ORIGINS_PROD = arrayOf(
            "https://tjenester.nav.no",
            "https://www.nav.no"
        )

        private val ALLOWED_ORIGINS_NON_PROD = arrayOf("*")
    }

//    Fra websecurityconfig:
// @Bean
// fun corsConfigurationSource(): CorsConfigurationSource {
//    val configuration = CorsConfiguration()
//    configuration.allowedOrigins = listOf(
//        "https://www.nav.no",
//        "https://www-q0.nav.no",
//        "https://www-q1.nav.no",
//        "https://sosialhjelp-innsyn-gcp.dev.nav.no",
//        "https://sosialhjelp-innsyn.labs.nais.io",
//        "http://localhost:3000",
//        "http://localhost:3001",
//        "http://localhost:3002",
//        "https://digisos.labs.nais.io",
//        "https://digisos-gcp.dev.nav.no",
//        "https://sosialhjelp-innsyn-q.dev.nav.no"
//    )
//    configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
//    configuration.allowedHeaders = listOf("Origin", "Content-Type", "Accept", "X-XSRF-TOKEN", "Authorization", "Nav-Call-Id")
//    configuration.allowCredentials = true
//    val source = UrlBasedCorsConfigurationSource()
//    source.registerCorsConfiguration("/**", configuration)
//    return source
// }

//    Fra CORSFilter:
// val httpResponse = servletResponse as HttpServletResponse
//    val origin = if (servletRequest is HttpServletRequest) (servletRequest.getHeader("Origin")) else null
//
//    if (!isRunningInProd() || ALLOWED_ORIGINS.contains(origin)) {
//        httpResponse.setHeader("Access-Control-Allow-Origin", origin)
//        httpResponse.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, X-XSRF-TOKEN, XSRF-TOKEN-INNSYN-API, Authorization, Nav-Call-Id")
//        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
//        httpResponse.setHeader("Access-Control-Allow-Credentials", "true")
//    }
//    filterChain.doFilter(servletRequest, httpResponse)
// }
}
