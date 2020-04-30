package no.nav.sbl.sosialhjelpinnsynapi.config

import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.util.function.Supplier


@Configuration
class RestConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder.requestFactory(MemorySafeRequestFactorySupplier()).build()
    }

    internal class MemorySafeRequestFactorySupplier : Supplier<ClientHttpRequestFactory?> {
        override fun get(): SimpleClientHttpRequestFactory {
            val requestFactory = SimpleClientHttpRequestFactory()
            requestFactory.setBufferRequestBody(false) // When sending large amounts of data via POST or PUT, it is recommended to change this property to false, so as not to run out of memory.
            return requestFactory
        }
    }

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