package no.nav.sosialhjelp.innsyn.pdl

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.reactor.awaitSingle
import no.nav.sosialhjelp.innsyn.pdl.dto.PdlPerson
import org.springframework.graphql.client.HttpGraphQlClient
import org.springframework.stereotype.Component

@Component
class PdlClient(
    private val pdlGraphQlClientFactory: PdlGraphQlClientFactory,
    private val pdlHttpGraphQlclient: HttpGraphQlClient,
) {
//    /** Henter en person fra PDL */
//    @CircuitBreaker(name = "pdl")
//    suspend fun getPerson(ident: String): PdlPerson =
//        pdlGraphQlClientFactory
//            .getClient(TokenUtils.getToken())
//            .documentName("hentPerson")
//            .variable("ident", ident)
//            .retrieve("hentPerson")
//            .toEntity(PdlPerson::class.java)
//            .awaitSingle()

    /** Henter en person fra PDL */
    @CircuitBreaker(name = "pdl")
    suspend fun getPerson(ident: String): PdlPerson =
        pdlHttpGraphQlclient
            .documentName("hentPerson")
            .variable("ident", ident)
            .retrieve("hentPerson")
            .toEntity(PdlPerson::class.java)
            .awaitSingle()
}
