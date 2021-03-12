package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_NAV_APIKEY
import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.nio.charset.StandardCharsets
import java.time.Duration

@Configuration
class RestConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate =
            builder.build()

    @Bean
    @Profile("!(mock | local)")
    fun stsRestTemplate(builder: RestTemplateBuilder): RestTemplate =
            builder
                    .basicAuthentication(System.getenv(SRVSOSIALHJELP_INNSYN_API_USERNAME), System.getenv(SRVSOSIALHJELP_INNSYN_API_PASSWORD), StandardCharsets.UTF_8)
                    .defaultHeader(HEADER_NAV_APIKEY, System.getenv(STSTOKEN_APIKEY))
                    .build()

    @Bean
    @Profile("!(mock | local)")
    fun pdlRestTemplate(builder: RestTemplateBuilder): RestTemplate =
            builder
                    .basicAuthentication(System.getenv(SRVSOSIALHJELP_INNSYN_API_USERNAME), System.getenv(SRVSOSIALHJELP_INNSYN_API_PASSWORD), StandardCharsets.UTF_8)
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofMinutes(1))
                    .defaultHeader(HEADER_NAV_APIKEY, System.getenv(PDL_APIKEY))
                    .build()

    @Bean
    fun objectMapperCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { jacksonObjectMapperBuilder ->
            jacksonObjectMapperBuilder.configure(objectMapper)
        }
    }

    @Bean
    fun myMessageConverter(reqAdapter: RequestMappingHandlerAdapter,
                           jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder): MappingJackson2HttpMessageConverter {
        // **replace previous MappingJackson converter**
        val converters = reqAdapter.messageConverters
        converters.removeIf { httpMessageConverter -> httpMessageConverter.javaClass == MappingJackson2HttpMessageConverter::class.java }

        val jackson = MappingJackson2HttpMessageConverter(objectMapper)
        converters.add(jackson)
        reqAdapter.messageConverters = converters
        return jackson
    }

    companion object {
        private const val SRVSOSIALHJELP_INNSYN_API_USERNAME: String = "SRVSOSIALHJELP_INNSYN_API_USERNAME"
        private const val SRVSOSIALHJELP_INNSYN_API_PASSWORD: String = "SRVSOSIALHJELP_INNSYN_API_PASSWORD"

        private const val STSTOKEN_APIKEY: String = "SOSIALHJELP_INNSYN_API_STSTOKEN_APIKEY_PASSWORD"
        private const val PDL_APIKEY: String = "SOSIALHJELP_INNSYN_API_PDL_APIKEY_PASSWORD"
    }
}