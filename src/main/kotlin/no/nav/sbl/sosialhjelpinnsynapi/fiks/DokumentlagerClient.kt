package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(DokumentlagerClient::class.java)

@Component
class DokumentlagerClient(clientProperties: ClientProperties,
                          private val restTemplate: RestTemplate = RestTemplate()) {

    private val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl
    private val mapper = jacksonObjectMapper()

    fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        val response = restTemplate.getForEntity("$baseUrl/dokumentlager/nedlasting/$dokumentlagerId", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return mapper.readValue(response.body!!, requestedClass)
        } else {
            log.warn("Noe feilet ved kall til Dokumentlager")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}