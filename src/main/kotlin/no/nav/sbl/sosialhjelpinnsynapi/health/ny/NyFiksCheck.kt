package no.nav.sbl.sosialhjelpinnsynapi.health.ny

import kotlinx.coroutines.runBlocking
import no.nav.sbl.sosialhjelpinnsynapi.client.idporten.IdPortenService
import no.nav.sbl.sosialhjelpinnsynapi.common.FiksException
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils
import no.nav.sbl.sosialhjelpinnsynapi.utils.logger
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.util.*

@Profile("!mock")
@Component
class NyFiksCheck(
        private val restTemplate: RestTemplate,
        private val clientProperties: ClientProperties,
        private val idPortenService: IdPortenService
) : DependencyCheck(
        DependencyType.REST,
        "Fiks Digisos API",
        clientProperties.fiksDigisosEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        try {
            val headers = HttpHeaders()
            val accessToken = runBlocking { idPortenService.requestToken() }
            headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken.token}")
            headers.set(IntegrationUtils.HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            headers.set(IntegrationUtils.HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)

            // later som kommuneInfo-kall er ping for selftest
            restTemplate.exchange("$address/digisos/api/v1/nav/kommune/0301", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        } catch (e: HttpStatusCodeException) {
            log.warn("Selftest - Fiks hentKommuneInfo feilet - ${e.statusCode} ${e.statusText}", e)
            throw FiksException(e.message, e)
        } catch (e: Exception) {
            log.warn("Selftest - Fiks hentKommuneInfo feilet", e)
            throw FiksException(e.message, e)
        }
    }

    companion object {
        private val log by logger()
    }
}