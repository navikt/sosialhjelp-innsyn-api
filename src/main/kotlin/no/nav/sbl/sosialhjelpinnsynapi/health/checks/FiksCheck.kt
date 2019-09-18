package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class FiksCheck(private val restTemplate: RestTemplate,
                private val clientProperties: ClientProperties) : AbstractDependencyCheck(
        DependencyType.REST,
        "Fiks Digisos API",
        clientProperties.fiksDigisosEndpointUrl,
        Importance.WARNING
) {

    override fun doCheck() {
        try {
            // som i FiksClientImpl
            val headers = HttpHeaders()
            headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
            headers.set(AUTHORIZATION, "token") // Token ?
            headers.set(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            headers.set(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)

            restTemplate.exchange("$address/digisos/api/v1/soknader/0", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java) // FIXME må ha ett reelt endepunkt å kalle
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke pinge Dokumentlager", e)
        }
    }
}