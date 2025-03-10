package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandler
import no.nav.sosialhjelp.innsyn.app.subjecthandler.SubjectHandlerUtils.getToken
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
    private val subjectHandler: SubjectHandler,
) {
    suspend fun getAdressebeskyttelse(): Boolean =
        pdlClient
            .getPerson(subjectHandler.getToken())
            .adressebeskyttelse
            .any { it.gradering in BEGRENSEDE_GRADERINGER }

    suspend fun getFornavn(): String =
        pdlClient
            .getPerson(subjectHandler.getToken())
            .navn
            .first()
            .fornavn

    suspend fun getIdentsByIdent(ident: String): List<String> = pdlClient.getIdentsByIdent(getToken())

    companion object {
        private val BEGRENSEDE_GRADERINGER =
            setOf(PdlGradering.FORTROLIG, PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND)
    }
}
