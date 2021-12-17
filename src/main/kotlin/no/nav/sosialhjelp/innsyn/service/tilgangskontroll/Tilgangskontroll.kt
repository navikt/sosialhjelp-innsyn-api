package no.nav.sosialhjelp.innsyn.service.tilgangskontroll

import no.nav.sosialhjelp.innsyn.client.pdl.PdlClient
import no.nav.sosialhjelp.innsyn.client.pdl.PdlPerson
import no.nav.sosialhjelp.innsyn.client.pdl.isKode6Or7
import no.nav.sosialhjelp.innsyn.common.PdlException
import no.nav.sosialhjelp.innsyn.common.TilgangskontrollException
import no.nav.sosialhjelp.innsyn.common.subjecthandler.SubjectHandlerUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.Locale

interface Tilgangskontroll {
    fun sjekkTilgang(token: String)
    fun hentTilgang(ident: String, token: String): Tilgang
}

@Profile("!local")
@Component
class TilgangskontrollService(
    private val pdlClient: PdlClient
) : Tilgangskontroll {

    override fun sjekkTilgang(token: String) {
        sjekkTilgang(SubjectHandlerUtils.getUserIdFromToken(), token)
    }

    fun sjekkTilgang(ident: String, token: String) {
        val hentPerson = hentPerson(ident, token)
        if (hentPerson != null && hentPerson.isKode6Or7()) {
            throw TilgangskontrollException("Bruker har ikke tilgang til innsyn")
        }
    }

    override fun hentTilgang(ident: String, token: String): Tilgang {
        val pdlPerson = hentPerson(ident, token)
        val harTilgang = !(pdlPerson != null && pdlPerson.isKode6Or7())
        return Tilgang(harTilgang, fornavn(pdlPerson))
    }

    private fun hentPerson(ident: String, token: String): PdlPerson? {
        return try {
            pdlClient.hentPerson(ident, token)?.hentPerson
        } catch (e: PdlException) {
            log.warn("PDL kaster feil -> gir midlertidig tilgang til ressurs")
            null
        }
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

@Profile("local")
@Component
class TilgangskontrollLocal : Tilgangskontroll {

    override fun sjekkTilgang(token: String) {
        // no-op
    }

    override fun hentTilgang(ident: String, token: String): Tilgang {
        return Tilgang(
            harTilgang = true,
            fornavn = "mockperson"
        )
    }
}

data class Tilgang(
    val harTilgang: Boolean,
    val fornavn: String
)
