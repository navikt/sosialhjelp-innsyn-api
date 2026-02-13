package no.nav.sosialhjelp.innsyn.pdl

import no.nav.sosialhjelp.innsyn.pdl.dto.PdlGradering
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlNavn
import no.nav.sosialhjelp.innsyn.valkey.AdressebeskyttelseCacheConfig
import no.nav.sosialhjelp.innsyn.valkey.PdlNavnCacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
) {
    @Cacheable(AdressebeskyttelseCacheConfig.CACHE_NAME)
    suspend fun getAdressebeskyttelse(personId: String): Boolean =
        pdlClient
            .getPerson(personId)
            .adressebeskyttelse
            .any { it.gradering in BEGRENSEDE_GRADERINGER }

    @Cacheable(PdlNavnCacheConfig.CACHE_NAME)
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
