package no.nav.sbl.sosialhjelpinnsynapi.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import org.apache.http.HttpHost
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(HttpClient::class.java)
val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

internal val defaultHttpClient = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = JacksonSerializer { objectMapper }
    }
    engine {
        customizeClient {
            val proxy = System.getenv("HTTP_PROXY")
            if (proxy != null) {
                setProxy(HttpHost(proxy.substringAfterLast("/").substringBeforeLast(":"), Integer.valueOf(proxy.substringAfterLast(":"))))
                log.info("proxy set")
            }
        }
    }
}
