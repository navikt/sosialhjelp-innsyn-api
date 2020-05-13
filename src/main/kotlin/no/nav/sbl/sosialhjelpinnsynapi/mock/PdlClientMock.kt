package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.consumer.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.consumer.pdl.PdlHentPerson
import no.nav.sbl.sosialhjelpinnsynapi.consumer.pdl.PdlPerson
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock | local")
@Component
class PdlClientMock : PdlClient {

    private val pdlMap = mutableMapOf<String, PdlHentPerson>()

    override fun hentPerson(ident: String): PdlHentPerson? {
        return pdlMap.getOrElse(ident, {
            val default = PdlHentPerson(
                    hentPerson = PdlPerson(
                            adressebeskyttelse = emptyList()
                    )
            )
            pdlMap[ident] = default
            default
        })
    }

    override fun ping() {

    }
}