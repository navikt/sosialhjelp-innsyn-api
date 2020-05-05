package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.common.NorgException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import no.nav.sbl.sosialhjelpinnsynapi.logger
import no.nav.sbl.sosialhjelpinnsynapi.utils.generateCallId
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

@Profile("!mock")
@Component
class NorgCheck(
        private val restTemplate: RestTemplate,
        clientProperties: ClientProperties
) : AbstractDependencyCheck(
        DependencyType.REST,
        "NORG2",
        clientProperties.norgEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        try {
            val norgApiKey = System.getenv("NORG_PASSWORD")
            val headers = HttpHeaders()
            headers.set("Nav-Call-Id", generateCallId())
            headers.set("x-nav-apiKey", norgApiKey)

            // samme kall som selftest i soknad-api
            restTemplate.exchange("$address/kodeverk/EnhetstyperNorg", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        } catch (e: HttpStatusCodeException) {
            log.warn("Selftest - noe feilet ved kall mot NORG - ${e.statusCode} ${e.statusText}", e)
            throw NorgException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            log.warn("Selftest - noe feilet ved kall mot NORG", e)
            throw NorgException(null, e.message, e)
        }
    }

    companion object {
        private val log by logger()
    }
}