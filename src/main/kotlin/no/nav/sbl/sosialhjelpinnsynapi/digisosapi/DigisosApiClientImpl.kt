package no.nav.sbl.sosialhjelpinnsynapi.digisosapi

import no.nav.sbl.soknadsosialhjelp.json.JsonSosialhjelpObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.*

private val log = LoggerFactory.getLogger(DigisosApiClient::class.java)

@Profile("!mock")
@Component
class DigisosApiClientImpl(clientProperties: ClientProperties, private val restTemplate: RestTemplate) : DigisosApiClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonIdKommune = clientProperties.fiksIntegrasjonIdKommune
    private val fiksIntegrasjonPassordKommune = clientProperties.fiksIntegrasjonPassordKommune
    private val mapper = JsonSosialhjelpObjectMapper.createObjectMapper()

    override fun postDigisosSakMedInnsyn(digisosSak: DigisosSak) {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set("IntegrasjonId", fiksIntegrasjonIdKommune)
        headers.set("IntegrasjonPassord", fiksIntegrasjonPassordKommune)
        val httpEntity = HttpEntity(mapper.writeValueAsString(digisosSak), headers)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/${digisosSak.fiksOrgId}/${digisosSak.fiksDigisosId}", HttpMethod.POST, httpEntity, String::class.java)


        if (response.statusCode.is2xxSuccessful) {
             log.info("Postet DigisosSak til Fiks")
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun postDigisosSakMedInnsynNy(digisosSak: DigisosSak) {
        val headers = HttpHeaders()
        headers.accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        headers.set("IntegrasjonId", fiksIntegrasjonIdKommune)
        headers.set("IntegrasjonPassord", fiksIntegrasjonPassordKommune)

        val httpEntity = HttpEntity(mapper.writeValueAsString(digisosSak), headers)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/${digisosSak.fiksOrgId}/ny", HttpMethod.POST, httpEntity, String::class.java)


        if (response.statusCode.is2xxSuccessful) {
             log.info("Postet DigisosSak til Fiks")
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

}