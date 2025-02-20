package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getToken
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
) {
    suspend fun getAdressebeskyttelseByIdent(ident: String): Boolean =
        pdlClient
            .getPersonByIdent(ident, getToken())
            .adressebeskyttelse.any { it.gradering in BEGRENSEDE_GRADERINGER }

    suspend fun getFornavnByIdent(ident: String): String =
        pdlClient
            .getPersonByIdent(ident, getToken())
            .navn.first().fornavn

    companion object {
        private val BEGRENSEDE_GRADERINGER =
            setOf(PdlGradering.FORTROLIG, PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND)
    }
}
