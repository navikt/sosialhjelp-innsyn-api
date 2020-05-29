package no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll

import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlPerson
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.isKode6Or7
import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.common.TilgangskontrollException
import org.springframework.stereotype.Component

@Component
class TilgangskontrollService(
        private val pdlClient: PdlClient
) {

    fun harTilgang(ident: String) {
        val hentPerson: PdlPerson = pdlClient.hentPerson(ident)?.hentPerson ?: throw PdlException(null, "PDL returnerte PdlPersonResponse.data = null")

        if (hentPerson.isKode6Or7()) {
            throw TilgangskontrollException("Bruker har ikke tilgang til innsyn")
        }
    }

}