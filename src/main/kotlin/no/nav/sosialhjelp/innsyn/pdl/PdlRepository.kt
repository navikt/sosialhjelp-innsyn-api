package no.nav.sosialhjelp.innsyn.pdl

import PDLPerson
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class PdlRepository (
    private val pdlClient: PdlClientV2
) {
    /** Henter en person fra PDL */
    @Cacheable("pdl")
    @CircuitBreaker(name = "pdl")
    suspend fun getPersonByPid(pid: String, token: String): PDLPerson = pdlClient
        .buildClient(token)
        .documentName("hentPerson")
        .variable("ident", pid)
        .retrieve("hentPerson")
        .toEntity(PDLPerson::class.java)
        .awaitSingle()

    /** Henter en liste over alle identer tilknyttet en person */
    @Cacheable("pdl")
    @CircuitBreaker(name = "pdl")
    suspend fun getPidHistoryByPid(pid: String, token: String): List<String> = pdlClient
        .buildClient(token)
        .documentName("hentIdenter")
        .variable("ident", pid)
        .retrieve("hentIdenter.identer[*].ident")
        .toEntityList(String::class.java)
        .awaitLast()
}
