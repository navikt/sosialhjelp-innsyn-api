package no.nav.sosialhjelp.innsyn.navenhet

import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import no.nav.sosialhjelp.innsyn.app.exceptions.NorgException
import no.nav.sosialhjelp.innsyn.app.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class NorgClientImpl(
    private val norgWebClient: WebClient,
) : NorgClient {
    @Cacheable("navenhet")
    @CircuitBreaker(name = "norg")
    @Retry(name = "norg")
    @Bulkhead(name = "norg")
    override suspend fun hentNavEnhet(enhetsnr: String): NavEnhet =
        withContext(Dispatchers.IO) {
            log.debug("Forsøker å hente Nav-enhet $enhetsnr fra NORG2")

            norgWebClient
                .get()
                .uri("/enhet/{enhetsnr}", enhetsnr)
                .accept(MediaType.APPLICATION_JSON)
                .header(IntegrationUtils.HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
                .retrieve()
                .bodyToMono<NavEnhet>()
                .onErrorMap(WebClientResponseException::class.java) { e ->
                    log.warn("Noe feilet ved kall mot NORG2 ${e.statusCode}", e)
                    NorgException(e.message, e)
                }.awaitSingle()
                .also { log.info("Hentet Nav-enhet $enhetsnr fra NORG2") }
        }

    companion object {
        private val log by logger()
    }
}
