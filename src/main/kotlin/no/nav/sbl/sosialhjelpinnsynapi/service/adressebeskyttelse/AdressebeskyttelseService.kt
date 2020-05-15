package no.nav.sbl.sosialhjelpinnsynapi.service.adressebeskyttelse

import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.isKode6Or7
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class AdressebeskyttelseService(
        private val pdlClient: PdlClient
) {

    fun isKode6Or7(ident: String): Boolean {
        return pdlClient.hentPerson(ident)?.isKode6Or7()
                ?: throw PdlException(HttpStatus.INTERNAL_SERVER_ERROR, "Noe uventet feilet ved uthenting ")
    }

}