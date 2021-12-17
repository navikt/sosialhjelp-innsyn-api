package no.nav.sosialhjelp.innsyn.client.pdl

import kotlinx.coroutines.runBlocking
import no.nav.sosialhjelp.innsyn.client.sts.StsClient
import no.nav.sosialhjelp.innsyn.client.tokendings.TokendingsService
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.BEARER
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.HEADER_TEMA
import no.nav.sosialhjelp.innsyn.utils.IntegrationUtils.TEMA_KOM
import no.nav.sosialhjelp.innsyn.utils.mdc.MDCUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

interface PdlHeadersService {
    fun getHeaders(ident: String, token: String): HttpHeaders
}

@Service
@Profile("!(dev-gcp-q)")
class PdlStsHeadersService(
    private val stsClient: StsClient,
) : PdlHeadersService {
    override fun getHeaders(ident: String, token: String): HttpHeaders {
        val stsToken = stsClient.token()
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set(IntegrationUtils.HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
        headers.set(IntegrationUtils.HEADER_CONSUMER_TOKEN, BEARER + stsToken)
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + stsToken)
        headers.set(HEADER_TEMA, TEMA_KOM)
        return headers
    }
}

@Service
@Profile("(dev-gcp-q)")
class PdlTokendingsHeadersService(
    private val tokendingsService: TokendingsService,
    @Value("\${pdl_audience}") private val pdlAudience: String
) : PdlHeadersService {
    override fun getHeaders(ident: String, token: String): HttpHeaders {
        val headers = HttpHeaders()
        runBlocking {
            val tokenXToken = tokendingsService.exchangeToken(ident, token, pdlAudience)

            headers.contentType = MediaType.APPLICATION_JSON
            headers.set(HttpHeaders.AUTHORIZATION, BEARER + tokenXToken)
            headers.set(HEADER_TEMA, TEMA_KOM)
        }
        return headers
    }
}
