package no.nav.sosialhjelp.innsyn.pdl

import com.nimbusds.jwt.JWTParser
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlPerson
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
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
    @Cacheable("pdlIdenter", keyGenerator = "pdlCacheKeyGenerator")
    @CircuitBreaker(name = "pdl")
    suspend fun getIdentsByIdent(token: String): List<String> =
        pdlGraphQlClientFactory
            .getClient(token)
            .documentName("hentIdenter")
            .variable("ident", getPersonidentFromToken(token))
            .retrieve("hentIdenter.identer[*].ident")
            .toEntityList(String::class.java)
            .awaitLast()

    companion object {
        private fun getPersonidentFromToken(token: String): String = JWTParser.parse(token).jwtClaimsSet.getStringClaim("pid")
    }
}
