package no.nav.sosialhjelp.innsyn.mock

import no.nav.sosialhjelp.innsyn.client.pdl.Adressebeskyttelse
import no.nav.sosialhjelp.innsyn.client.pdl.Gradering
import no.nav.sosialhjelp.innsyn.client.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.client.pdl.PdlHentPerson
import no.nav.sosialhjelp.innsyn.client.pdl.PdlNavn
import no.nav.sosialhjelp.innsyn.client.pdl.PdlPerson
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
        return pdlMap.getOrElse(
            ident,
            {
                val default = PdlHentPerson(
                    hentPerson = PdlPerson(
                        adressebeskyttelse = vanlig,
                        navn = navn
                    )
                )
                pdlMap[ident] = default
                default
            }
        )
    }

    override fun ping() {
        // no-op
    }
}
