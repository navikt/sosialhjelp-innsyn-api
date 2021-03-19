package no.nav.sosialhjelp.innsyn.config

import no.nav.sosialhjelp.innsyn.utils.objectMapper
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@Configuration
class RestConfig {

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
}