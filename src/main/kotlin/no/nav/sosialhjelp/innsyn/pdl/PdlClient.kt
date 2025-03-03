package no.nav.sosialhjelp.innsyn.pdl

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import no.nav.sosialhjelp.innsyn.pdl.PdlCacheKeyGenerator.Companion.getPersonidentFromToken
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlPerson
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class PdlClient(
    private val pdlGraphQlClientFactory: PdlGraphQlClientFactory,
) {
    /** Henter en person fra PDL */
    @Cacheable("pdlPerson", keyGenerator = "pdlCacheKeyGenerator")
    @CircuitBreaker(name = "pdl")
    suspend fun getPerson(token: String): PdlPerson =
        pdlGraphQlClientFactory
            .getClient(token)
            .documentName("hentPerson")
            .variable("ident", getPersonidentFromToken(token))
            .retrieve("hentPerson")
            .toEntity(PdlPerson::class.java)
            .awaitSingle()

    /** Henter en liste med alle identer tilknyttet en person */
    @Cacheable("pdlHistoriskeIdenter", keyGenerator = "pdlCacheKeyGenerator")
    @CircuitBreaker(name = "pdl")
    suspend fun getIdentsByIdent(token: String): List<String> =
        pdlGraphQlClientFactory
            .getClient(token)
            .documentName("hentIdenter")
            .variable("ident", getPersonidentFromToken(token))
            .retrieve("hentIdenter.identer[*].ident")
            .toEntityList(String::class.java)
            .awaitLast()
}
