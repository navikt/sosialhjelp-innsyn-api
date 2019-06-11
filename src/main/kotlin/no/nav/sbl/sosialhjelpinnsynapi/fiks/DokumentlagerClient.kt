package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(DokumentlagerClient::class.java)

@Component
class DokumentlagerClient(private val clientProperties: ClientProperties,
                          private val restTemplate: RestTemplate = RestTemplate()) {

    private val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl

    fun hentDokument(dokumentlagerId: String): String {
        val response = restTemplate.getForEntity("$baseUrl/dokumentlager/nedlasting/$dokumentlagerId", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til Dokumentlager")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}