package no.nav.sbl.sosialhjelpinnsynapi.service.tilgangskontroll

import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlClient
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.PdlPerson
import no.nav.sbl.sosialhjelpinnsynapi.client.pdl.isKode6Or7
import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.common.TilgangskontrollException
import no.nav.sbl.sosialhjelpinnsynapi.common.subjecthandler.SubjectHandlerUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.stereotype.Component

@Component
class TilgangskontrollService(
        private val pdlClient: PdlClient
) {

    fun sjekkTilgang() {
        sjekkTilgang(SubjectHandlerUtils.getUserIdFromToken())
    }

    fun sjekkTilgang(ident: String) {
        val hentPerson = hentPerson(ident)
        if (hentPerson != null && hentPerson.isKode6Or7()) {
            throw TilgangskontrollException("Bruker har ikke tilgang til innsyn")
        }
    }

    fun hentTilgang(ident: String): Tilgang {
        val pdlPerson = hentPerson(ident)
        val harTilgang = !(pdlPerson != null && pdlPerson.isKode6Or7())
        val fornavn = pdlPerson?.navn?.first()?.fornavn?.toLowerCase()?.capitalize() ?: ""
        return Tilgang(harTilgang, fornavn)
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

data class Tilgang(
        val harTilgang: Boolean,
        val fornavn: String
)