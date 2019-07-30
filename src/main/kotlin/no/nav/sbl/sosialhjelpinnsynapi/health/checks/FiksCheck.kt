package no.nav.sbl.sosialhjelpinnsynapi.health.checks

import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.AbstractDependencyCheck
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.DependencyType
import no.nav.sbl.sosialhjelpinnsynapi.health.selftest.Importance
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class FiksCheck (private val restTemplate: RestTemplate,
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
            headers.set(HttpHeaders.AUTHORIZATION, "token") // Token ?
            headers.set("IntegrasjonId", "046f44cc-4fbd-45f6-90f7-d2cc8a3720d2")
            headers.set("IntegrasjonPassord", clientProperties.fiksIntegrasjonpassord)

            restTemplate.exchange("$address/digisos/api/v1/soknader/0", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java) // må ha ett reelt endepunkt å kalle
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke pinge Dokumentlager", e)
        }
    }
}