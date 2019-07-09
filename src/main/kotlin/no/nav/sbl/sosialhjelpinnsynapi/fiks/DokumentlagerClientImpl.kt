package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(DokumentlagerClient::class.java)

private const val dokumentlager_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class DokumentlagerClientImpl(clientProperties: ClientProperties,
                              private val restTemplate: RestTemplate) : DokumentlagerClient {

    private val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl
    private val mapper = JsonSosialhjelpObjectMapper.createObjectMapper()

    override fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>): Any {
        if (dokumentlagerId == dokumentlager_stub_id && requestedClass == JsonDigisosSoker::class.java) {
            log.info("Henter stub - dokumentlagerId $dokumentlagerId")
            return mapper.readValue(ok_komplett_jsondigisossoker_response, requestedClass)
        }

        val response = restTemplate.getForEntity("$baseUrl/dokumentlager/nedlasting/$dokumentlagerId", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet dokument (${requestedClass.simpleName}) fra dokumentlager, dokumentlagerId $dokumentlagerId")
            return mapper.readValue(response.body!!, requestedClass)
        } else {
            log.warn("Noe feilet ved kall til Dokumentlager")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}
