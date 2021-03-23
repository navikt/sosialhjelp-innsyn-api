package no.nav.sosialhjelp.innsyn.client.pdl

import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.innsyn.client.sts.StsClient
import no.nav.sosialhjelp.innsyn.common.PdlException
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_CONSUMER_TOKEN
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_TEMA
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.TEMA_KOM
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.forwardHeaders
import no.nav.sosialhjelp.innsyn.utils.logger
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils.CALL_ID
import no.nav.sosialhjelp.kotlin.utils.retry
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Optional
import java.util.stream.Collectors

interface PdlClient {

    fun hentPerson(ident: String): PdlHentPerson?

    fun ping()
}

@Profile("!(mock | local)")
@Component
class PdlClientImpl(
    private val pdlWebClient: WebClient,
    private val stsClient: StsClient,
) : PdlClient {

    override fun hentPerson(ident: String): PdlHentPerson? {
        val query = getResourceAsString("/pdl/hentPerson.graphql").replace("[\n\r]", "")
        try {
            val pdlPersonResponse = runBlocking {
                retry(
                    attempts = RETRY_ATTEMPTS,
                    initialDelay = INITIAL_DELAY,
                    maxDelay = MAX_DELAY,
                    retryableExceptions = arrayOf(WebClientResponseException::class)
                ) {
                    pdlWebClient.post()
                        .headers { it.addAll(headers()) }
                        .bodyValue(PdlRequest(query, Variables(ident)))
                        .retrieve()
                        .awaitBody<PdlPersonResponse>()
                }
            }

            checkForPdlApiErrors(pdlPersonResponse)

            return pdlPersonResponse.data
        } catch (e: WebClientResponseException) {
            log.error("PDL - noe feilet, status=${e.rawStatusCode} ${e.statusText}", e)
            throw PdlException(e.message!!)
        }
    }

    override fun ping() {
        pdlWebClient.options()
            .retrieve()
            .bodyToMono<String>()
            .doOnError { e ->
                log.error("PDL - ping feilet", e)
            }
    }

    private fun getResourceAsString(path: String) = this::class.java.getResource(path).readText()

    private fun headers(): HttpHeaders {
        val stsToken: String = stsClient.token()

        val headers = forwardHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(HEADER_CALL_ID, MDCUtils.get(CALL_ID))
        headers.set(HEADER_CONSUMER_TOKEN, BEARER + stsToken)
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + stsToken)
        headers.set(HEADER_TEMA, TEMA_KOM)
        return headers
    }

    private fun checkForPdlApiErrors(response: PdlPersonResponse?) {
        Optional.ofNullable(response)
            .map(PdlPersonResponse::errors)
            .ifPresent { handleErrors(it) }
    }

    private fun handleErrors(errors: List<PdlError>) {
        val errorList = errors.stream()
            .map { it.message + "(feilkode: " + it.extensions.code + ")" }
            .collect(Collectors.toList())
        throw PdlException(errorMessage(errorList))
    }

    private fun errorMessage(errors: List<String>): String =
        "Error i respons fra pdl-api: ${errors.joinToString { it }}"

    companion object {
        private val log by logger()

        private const val RETRY_ATTEMPTS = 5
        private const val INITIAL_DELAY = 100L
        private const val MAX_DELAY = 2000L
    }
}