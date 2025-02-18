package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getToken
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
) {
    private val begrensedeGraderinger =
        listOf(PdlGradering.FORTROLIG, PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND)

    suspend fun getAdressebeskyttelseByIdent(ident: String): Boolean =
        pdlClient
            .getPersonByIdent(ident, getToken())
            .adressebeskyttelse.any { begrensedeGraderinger.contains(it.gradering) }

    suspend fun getFornavnByIdent(ident: String): String =
        pdlClient
            .getPersonByIdent(ident, getToken())
            .navn.first().fornavn
}
