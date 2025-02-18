package no.nav.sosialhjelp.innsyn.pdl

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlPerson
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class PdlClient(
    private val pdlGraphQlClientBuilder: PdlGraphQlClientBuilder,
) {
    /** Henter en person fra PDL */
    @Cacheable("pdl")
    @CircuitBreaker(name = "pdl")
    suspend fun getPersonByIdent(
        ident: String,
        token: String,
    ): PdlPerson =
        pdlGraphQlClientBuilder
            .buildClient(token)
            .documentName("hentPerson")
            .variable("ident", ident)
            .retrieve("hentPerson")
            .toEntity(PdlPerson::class.java)
            .awaitSingle()

    /** Henter en liste med alle identer tilknyttet en person */
    @Cacheable("pdl")
    @CircuitBreaker(name = "pdl")
    suspend fun getIdentsByIdent(
        ident: String,
        token: String,
    ): List<String> =
        pdlGraphQlClientBuilder
            .buildClient(token)
            .documentName("hentIdenter")
            .variable("ident", ident)
            .retrieve("hentIdenter.identer[*].ident")
            .toEntityList(String::class.java)
            .awaitLast()
}
