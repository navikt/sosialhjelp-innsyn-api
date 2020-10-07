package no.nav.sbl.sosialhjelpinnsynapi.mock

import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.Adressebeskyttelse
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.Gradering
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlHentPerson
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlNavn
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlPerson
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("mock | local")
@Component
class PdlClientMock : PdlClient {

    private val pdlMap = mutableMapOf<String, PdlHentPerson>()
    private val kode7 = listOf(Adressebeskyttelse(Gradering.FORTROLIG))
    private val kode6 = listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG))
    private val kode6_utland = listOf(Adressebeskyttelse(Gradering.STRENGT_FORTROLIG_UTLAND))
    private val navn = listOf(PdlNavn("KREATIV"), PdlNavn("NATA"))
    private val vanlig = emptyList<Adressebeskyttelse>()

    override fun hentPerson(ident: String): PdlHentPerson? {
        return pdlMap.getOrElse(ident, {
            val default = PdlHentPerson(
                    hentPerson = PdlPerson(
                            adressebeskyttelse = vanlig,
                            navn = navn
                    )
            )
            pdlMap[ident] = default
            default
        })
    }

    override fun ping() {

    }
}