package no.nav.sbl.sosialhjelpinnsynapi.fiks

import no.nav.sbl.soknadsosialhjelp.digisos.soker.JsonDigisosSoker
import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.error.exceptions.DokumentlagerException
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_ID
import no.nav.sbl.sosialhjelpinnsynapi.utils.IntegrationUtils.HEADER_INTEGRASJON_PASSORD
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.io.InputStream
import java.util.*

private val log = LoggerFactory.getLogger(DokumentlagerClient::class.java)

private const val dokumentlager_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class DokumentlagerClientImpl(private val clientProperties: ClientProperties,
                              private val restTemplate: RestTemplate) : DokumentlagerClient {

    private val baseUrl = clientProperties.fiksDokumentlagerEndpointUrl
    private val mapper = JsonSosialhjelpObjectMapper.createObjectMapper()

    override fun hentDokument(dokumentlagerId: String, requestedClass: Class<out Any>, token: String): Any {
        if (dokumentlagerId == dokumentlager_stub_id && requestedClass == JsonDigisosSoker::class.java) {
            log.info("Henter stub - dokumentlagerId $dokumentlagerId")
            return mapper.readValue(ok_komplett_jsondigisossoker_response, requestedClass)
        }

        try {
            val headers = HttpHeaders()
            headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON_UTF8)
            headers.set(AUTHORIZATION, token)
            headers.set(HEADER_INTEGRASJON_ID, clientProperties.fiksIntegrasjonId)
            headers.set(HEADER_INTEGRASJON_PASSORD, clientProperties.fiksIntegrasjonpassord)
            val response = restTemplate.exchange("$baseUrl/dokumentlager/nedlasting/$dokumentlagerId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.info("Hentet dokument (${requestedClass.simpleName}) fra dokumentlager, dokumentlagerId $dokumentlagerId")
                return mapper.readValue(response.body!!, requestedClass)
            } else {
                log.error("Noe feilet ved kall til Dokumentlager")
                throw DokumentlagerException(response.statusCode, "something went wrong")
            }
        } catch (e: RestClientResponseException) {
            throw DokumentlagerException(HttpStatus.valueOf(e.rawStatusCode), e.responseBodyAsString)
        }
    }
}
