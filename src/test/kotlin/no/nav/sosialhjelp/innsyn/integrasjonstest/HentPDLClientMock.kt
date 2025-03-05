package no.nav.sosialhjelp.innsyn.integrasjonstest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClientOld
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class HentPDLClientMock : PdlClientOld {
    val mapper: ObjectMapper =
        jacksonObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)

    override suspend fun hentPerson(
        ident: String,
        token: Token,
    ): PdlHentPerson {
        val resourceAsStream = ClassLoader.getSystemResourceAsStream("pdl/pdlPersonResponse.json")

        assertThat(resourceAsStream).isNotNull

        return mapper.readValue<PdlHentPerson>(resourceAsStream!!)
    }

    override suspend fun hentIdenter(
        ident: String,
        token: Token,
    ): List<String> {
//      ikke i bruk
        return emptyList()
    }

    override fun ping() {
//        ikke i bruk
    }
}
