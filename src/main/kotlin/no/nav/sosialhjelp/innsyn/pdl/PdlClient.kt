package no.nav.sosialhjelp.innsyn.pdl

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import no.nav.sosialhjelp.innsyn.app.token.TokenUtils
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlPerson
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class PdlClient(
    private val pdlGraphQlClientFactory: PdlGraphQlClientFactory,
) {
    /** Henter en person fra PDL */
    @CircuitBreaker(name = "pdl")
    suspend fun getPerson(ident: String): PdlPerson =
        pdlGraphQlClientFactory
            .getClient(TokenUtils.getToken())
            .documentName("hentPerson")
            .variable("ident", ident)
            .retrieve("hentPerson")
            .toEntity(PdlPerson::class.java)
            .awaitSingle()

    /** Henter en liste med alle identer tilknyttet en person */
    @Cacheable("pdlHistoriskeIdenter")
    @CircuitBreaker(name = "pdl")
    suspend fun getIdentsByIdent(ident: String): List<String> =
        pdlGraphQlClientFactory
            .getClient(TokenUtils.getToken())
            .documentName("hentIdenter")
            .variable("ident", TokenUtils.getUserIdFromToken())
            .retrieve("hentIdenter.identer[*].ident")
            .toEntityList(String::class.java)
            .awaitLast()
}
