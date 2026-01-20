package no.nav.sosialhjelp.innsyn.utils

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

val sosialhjelpJsonMapper: JsonMapper =
    JsonSosialhjelpObjectMapper
        .createJsonMapperBuilder()
        .addModule(kotlinModule())
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .build()

fun WebClient.Builder.configureCodecs(): WebClient.Builder {
    codecs {
        it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)
        it.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(sosialhjelpJsonMapper))
        it.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(sosialhjelpJsonMapper))
    }

    return this
}
