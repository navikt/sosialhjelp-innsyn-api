package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.sbl.sosialhjelpinnsynapi.utils.objectmapper
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@Configuration
class RestConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate =
            builder.build()

    @Bean
    fun objectMapperCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { jacksonObjectMapperBuilder ->
            jacksonObjectMapperBuilder.configure(objectmapper)
        }
    }

    @Bean
    fun myMessageConverter(reqAdapter: RequestMappingHandlerAdapter,
                           jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder): MappingJackson2HttpMessageConverter {
        // **replace previous MappingJackson converter**
        val converters = reqAdapter.messageConverters
        converters.removeIf { httpMessageConverter -> httpMessageConverter.javaClass == MappingJackson2HttpMessageConverter::class.java }

        val jackson = MappingJackson2HttpMessageConverter(objectmapper)
        converters.add(jackson)
        reqAdapter.messageConverters = converters
        return jackson
    }

}