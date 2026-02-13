package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlNavn
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
) {
    @Cacheable("pdlAdressebeskyttelse")
    suspend fun getAdressebeskyttelse(personId: String): Boolean =
        pdlClient
            .getPerson(personId)
            .adressebeskyttelse
            .any { it.gradering in BEGRENSEDE_GRADERINGER }

    @Cacheable("pdlNavn")
    suspend fun getNavn(personId: String): PdlNavn =
        pdlClient
            .getPerson(personId)
            .navn
            .first()

    companion object {
        private val BEGRENSEDE_GRADERINGER =
            setOf(PdlGradering.FORTROLIG, PdlGradering.STRENGT_FORTROLIG, PdlGradering.STRENGT_FORTROLIG_UTLAND)
    }
}
