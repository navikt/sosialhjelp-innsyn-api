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

val objectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())
    .configure(SerializationFeature.INDENT_OUTPUT, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

internal val defaultHttpClient = HttpClient(Apache) {
    install(JsonFeature) {
        serializer = JacksonSerializer { objectMapper }
    }
    engine{
        customizeClient {
            setProxy(HttpHost(System.getenv("http_proxy").split(":")[0], Integer.valueOf(System.getenv("http_proxy").split(":")[1])))
        }
    }
}
