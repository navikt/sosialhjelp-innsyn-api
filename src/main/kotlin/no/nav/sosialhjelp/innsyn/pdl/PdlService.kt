package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
) {
    suspend fun getAdressebeskyttelse(): Boolean =
        pdlClient
            .getPerson(TokenUtils.getUserIdFromToken())
            .adressebeskyttelse
            .any { it.gradering in BEGRENSEDE_GRADERINGER }

    suspend fun getFornavn(): String =
        pdlClient
            .getPerson(TokenUtils.getUserIdFromToken())
            .navn
            .first()
            .fornavn

    suspend fun getIdentsByIdent(ident: String): List<String> = pdlClient.getIdentsByIdent(ident)

    companion object {
        private val BEGRENSEDE_GRADERINGER =
            setOf(PdlGradering.FORTROLIG, PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND)
    }
}
