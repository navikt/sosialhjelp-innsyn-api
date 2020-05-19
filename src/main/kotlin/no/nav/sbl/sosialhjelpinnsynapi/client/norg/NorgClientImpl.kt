package no.nav.sbl.sosialhjelpinnsynapi.client.norg

import no.nav.sbl.sosialhjelpinnsynapi.common.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.NavEnhet
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_CALL_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_NAV_APIKEY
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.mdc.MDCUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.objectMapper
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate


@Profile("!(mock | local)")
@Component
class NorgClientImpl(
        clientProperties: ClientProperties,
        private val restTemplate: RestTemplate
) : NorgClient {

    private val baseUrl = clientProperties.norgEndpointUrl

    override fun hentNavEnhet(enhetsnr: String): NavEnhet {
        val norgApiKey = System.getenv("NORG_PASSWORD")
        val headers = HttpHeaders()
        headers.set(HEADER_CALL_ID, MDCUtils.get(MDCUtils.CALL_ID))
        headers.set(HEADER_NAV_APIKEY, norgApiKey)
        try {
            log.debug("Forsøker å hente NAV-enhet $enhetsnr fra NORG2")
            val urlTemplate = "$baseUrl/enhet/{enhetsnr}"
            val response = restTemplate.exchange(urlTemplate, HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java, enhetsnr)

            log.info("Hentet NAV-enhet $enhetsnr fra NORG2")
            return objectMapper.readValue(response.body!!, NavEnhet::class.java)

        } catch (e: HttpStatusCodeException) {
            log.warn("Noe feilet ved kall mot NORG - ${e.statusCode} ${e.statusText}", e)
            throw NorgException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Noe feilet ved kall mot NORG", e)
            throw NorgException(null, e.message, e)
        }
    }

    companion object {
        private val log by logger()
    }
}
