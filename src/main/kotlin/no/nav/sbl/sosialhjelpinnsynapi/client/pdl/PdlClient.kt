package no.nav.sbl.sosialhjelpinnsynapi.client.pdl

import no.nav.sbl.sosialhjelpinnsynapi.client.sts.StsClient
import no.nav.sbl.sosialhjelpinnsynapi.common.PdlException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.BEARER
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CONSUMER_TOKEN
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_TEMA
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.TEMA_KOM
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils.getCallId
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

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
            val response = pdlRestTemplate.exchange(baseurl, HttpMethod.POST, requestEntity, PdlPersonResponse::class.java)

            val pdlPersonResponse: PdlPersonResponse = response.body!!
            if (pdlPersonResponse.errors != null && pdlPersonResponse.errors.isNotEmpty()) {
                pdlPersonResponse.errors
                        .forEach { log.error("PDL - noe feilet. Message=${it.message}, path=${it.path}, code=${it.extensions.code}, classification=${it.extensions.classification}") }
                val firstError = pdlPersonResponse.errors[0]
                throw PdlException(
                        firstError.extensions.code?.toUpperCase()?.let { HttpStatus.valueOf(it) },
                        "Message: ${firstError.message}, Classification: ${firstError.extensions.classification}"
                )
            }
            return pdlPersonResponse.data
        } catch (e: RestClientResponseException) {
            log.error("PDL - ${e.rawStatusCode} ${e.statusText} feil ved henting av navn, requesturl: $baseurl", e)
            throw PdlException(HttpStatus.valueOf(e.rawStatusCode), e.message)
        }
    }

    override fun ping() {
        try {
            pdlRestTemplate.exchange(baseurl, HttpMethod.OPTIONS, HttpEntity(null, null), String::class.java)
        } catch (e: RestClientException) {
            log.error("PDL - ping feilet, requesturl: $baseurl", e)
            throw e
        }
    }

    private fun getResourceAsString(path: String) = this::class.java.getResource(path).readText()

    private fun createRequestEntity(request: PdlRequest): HttpEntity<PdlRequest> {
        val stsToken: String = stsClient.token()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(HEADER_CALL_ID, getCallId())
        headers.set(HEADER_CONSUMER_TOKEN, BEARER + stsToken)
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + stsToken)
        headers.set(HEADER_TEMA, TEMA_KOM)
        return HttpEntity(request, headers)
    }

    companion object {
        private val log by logger()
    }
}