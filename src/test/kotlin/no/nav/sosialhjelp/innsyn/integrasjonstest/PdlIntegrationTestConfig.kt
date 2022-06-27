package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.sosialhjelp.innsyn.client.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.client.pdl.PdlHentPerson
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.nio.charset.StandardCharsets

@Configuration
open class PdlIntegrationTestConfig {

    /**
     * overskriver pdlHentPersonConsumer for itester
     */
    @Primary
    @Bean
    open fun pdlClient(): PdlClient {
        return HentPDLClientMock()
    }
}

class HentPDLClientMock : PdlClient {

    val mapper: ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)

    override suspend fun hentPerson(ident: String, token: String): PdlHentPerson {
        val resourceAsStream = ClassLoader.getSystemResourceAsStream("pdl/pdlPersonResponse.json")

        assertThat(resourceAsStream).isNotNull
        val jsonString = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8)

        val pdlPersonResponse = mapper.readValue<PdlHentPerson>(jsonString)

        return pdlPersonResponse
    }

    override fun hentIdenter(ident: String, token: String): List<String> {
//      ikke i bruk
        return emptyList()
    }

    override fun ping() {
//        ikke i bruk
    }
}
