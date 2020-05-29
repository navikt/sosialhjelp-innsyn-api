package no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll

import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlPerson
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.isKode6Or7
import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.common.TilgangskontrollException
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.stereotype.Component

@Component
class TilgangskontrollService(
        private val pdlClient: PdlClient
) {

    fun sjekkTilgang(ident: String) {
        val hentPerson = hentPerson(ident)
        if (hentPerson != null && hentPerson.isKode6Or7()) {
            throw TilgangskontrollException("Bruker har ikke tilgang til innsyn")
        }
    }

    fun harTilgang(ident: String): Boolean {
        val hentPerson = hentPerson(ident)
        return !(hentPerson != null && hentPerson.isKode6Or7())
    }

    private fun hentPerson(ident: String): PdlPerson? {
        return try {
            pdlClient.hentPerson(ident)?.hentPerson
        } catch (e: PdlException) {
            log.warn("PDL kaster feil -> gir midlertidig tilgang til ressurs")
            null
        }
    }

    companion object {
        private val log by logger()
    }
}