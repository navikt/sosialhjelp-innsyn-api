package no.nav.sosialhjelp.innsyn.integrasjonstest

import no.nav.sosialhjelp.innsyn.app.token.Token
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClientOld
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlHentPerson
import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue

@Configuration
class PdlIntegrationTestConfig {
    /**
     * overskriver pdlHentPersonConsumer for itester
     */
    @Primary
    @Bean
    fun pdlClientOld(): PdlClientOld = HentPDLClientMock()
}

class HentPDLClientMock : PdlClientOld {
    val mapper: JsonMapper =
        jacksonMapperBuilder()
            .addModule(kotlinModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build()

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
