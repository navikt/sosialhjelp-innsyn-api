package no.nav.sbl.sosialhjelpinnsynapi.client.pdl

import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.client.sts.StsClient
import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.BEARER
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CONSUMER_TOKEN
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_TEMA
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.TEMA_KOM
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.forwardHeaders
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils.CALL_ID
import no.nav.sosialhjelp.kotlin.utils.retry
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.util.Optional
import java.util.stream.Collectors

interface PdlClient {

    fun hentPerson(ident: String): PdlHentPerson?

    fun ping()
}

@Profile("!(mock | local)")
@Component
class PdlClientImpl(
        clientProperties: ClientProperties,
        private val pdlRestTemplate: RestTemplate,
        private val stsClient: StsClient
) : PdlClient {

    private val baseurl = clientProperties.pdlEndpointUrl

    override fun hentPerson(ident: String): PdlHentPerson? {
        val query = getResourceAsString("/pdl/hentPerson.graphql").replace("[\n\r]", "")
        try {
            val requestEntity = createRequestEntity(PdlRequest(query, Variables(ident)))

            val response = runBlocking {
                retry(
                        attempts = RETRY_ATTEMPTS,
                        initialDelay = INITIAL_DELAY,
                        maxDelay = MAX_DELAY,
                        retryableExceptions = arrayOf(HttpServerErrorException::class)
                ) {
                    pdlRestTemplate.exchange(baseurl, HttpMethod.POST, requestEntity, PdlPersonResponse::class.java)
                }
            }

            val pdlPersonResponse: PdlPersonResponse = response.body!!

            checkForPdlApiErrors(pdlPersonResponse)

            return pdlPersonResponse.data
        } catch (e: RestClientResponseException) {
            log.error("PDL - noe feilet, status=${e.rawStatusCode} ${e.statusText}", e)
            throw PdlException(e.message!!)
        }
    }

    override fun ping() {
        try {
            pdlRestTemplate.exchange(baseurl, HttpMethod.OPTIONS, HttpEntity(null, null), String::class.java)
        } catch (e: RestClientException) {
            log.error("PDL - ping feilet", e)
            throw e
        }
    }

    private fun getResourceAsString(path: String) = this::class.java.getResource(path).readText()

    private fun createRequestEntity(request: PdlRequest): HttpEntity<PdlRequest> {
        val stsToken: String = stsClient.token()

        val headers = forwardHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(HEADER_CALL_ID, MDCUtils.get(CALL_ID))
        headers.set(HEADER_CONSUMER_TOKEN, BEARER + stsToken)
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + stsToken)
        headers.set(HEADER_TEMA, TEMA_KOM)
        return HttpEntity(request, headers)
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