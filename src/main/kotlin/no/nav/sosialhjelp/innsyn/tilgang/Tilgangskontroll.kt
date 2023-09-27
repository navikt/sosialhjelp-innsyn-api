package no.nav.sosialhjelp.innsyn.tilgang

import no.nav.sosialhjelp.api.fiks.DigisosSak
import no.nav.sosialhjelp.innsyn.app.exceptions.PdlException
import no.nav.sosialhjelp.innsyn.app.exceptions.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.tilgang.pdl.PdlPerson
import no.nav.sosialhjelp.innsyn.tilgang.pdl.isKode6Or7
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.Locale

interface Tilgangskontroll {
    fun sjekkTilgang(token: String)
    fun hentTilgang(ident: String, token: String): Tilgang
    fun verifyDigisosSakIsForCorrectUser(digisosSak: DigisosSak)
}

@Component
class TilgangskontrollService(
    @Value("\${login_api_idporten_clientid}") private val loginApiClientId: String,
    private val environment: Environment,
    private val pdlClient: PdlClient
) : Tilgangskontroll {

    override fun sjekkTilgang(token: String) {
        if (!environment.activeProfiles.contains("idporten")) {
            if (SubjectHandlerUtils.getClientId() != loginApiClientId) throw TilgangskontrollException("Feil clientId")
        }
        sjekkTilgang(SubjectHandlerUtils.getUserIdFromToken(), token)
    }

    fun sjekkTilgang(ident: String, token: String) {
        val hentPerson = hentPerson(ident, token)
        if (hentPerson != null && hentPerson.isKode6Or7()) {
            throw TilgangskontrollException("Bruker har ikke tilgang til innsyn")
        }
    }

    override fun hentTilgang(ident: String, token: String): Tilgang {
        val pdlPerson = hentPerson(ident, token) ?: return Tilgang(false, "")
        return Tilgang(!pdlPerson.isKode6Or7(), fornavn(pdlPerson))
    }

    private fun hentPerson(ident: String, token: String): PdlPerson? {
        return try {
            pdlClient.hentPerson(ident, token)?.hentPerson
        } catch (e: PdlException) {
            log.warn("PDL kaster feil -> midlertidig ikke tilgang", e)
            null
        }
    }

    override fun verifyDigisosSakIsForCorrectUser(digisosSak: DigisosSak) {
        val gyldigeIdenter = pdlClient.hentIdenter(SubjectHandlerUtils.getUserIdFromToken(), SubjectHandlerUtils.getToken())
        if (gyldigeIdenter?.contains(digisosSak.sokerFnr) != true)
            throw TilgangskontrollException("digisosSak hører ikke til rett person")
    }

    private fun fornavn(pdlPerson: PdlPerson?): String {
        val fornavn = pdlPerson?.navn?.firstOrNull()?.fornavn?.lowercase()?.replaceFirstChar { it.titlecase(Locale.getDefault()) } ?: ""
        if (fornavn.isEmpty()) {
            log.warn("PDL har ingen fornavn på brukeren. Dette gir ingen feil hos oss. Kontakt gjerne PDL-teamet, siden datakvaliteten på denne brukeren er dårlig.")
        }
        return fornavn
    }

    companion object {
        private val log by logger()
    }
}

data class Tilgang(
    val harTilgang: Boolean,
    val fornavn: String
)
