package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.config.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.Collections.singletonList


private val log = LoggerFactory.getLogger(FiksClientImpl::class.java)

private const val digisos_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Profile("!mock")
@Component
class FiksClientImpl(clientProperties: ClientProperties,
                     private val restTemplate: RestTemplate) : FiksClient {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val fiksIntegrasjonid = clientProperties.fiksIntegrasjonId
    private val fiksIntegrasjonpassord = clientProperties.fiksIntegrasjonpassord
    private val mapper = jacksonObjectMapper()


    override fun hentDigisosSak(digisosId: String, token: String): DigisosSak {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", fiksIntegrasjonid)
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)

        log.info("Forsøker å hente digisosSak fra $baseUrl/digisos/api/v1/soknader/$digisosId")
        if (digisosId == digisos_stub_id) {
            log.info("Hentet stub - digisosId $digisosId")
            return mapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        }
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader/$digisosId", HttpMethod.GET, HttpEntity<Nothing>(headers), String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet DigisosSak $digisosId fra Fiks")
            return mapper.readValue(response.body!!, DigisosSak::class.java)
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun hentAlleDigisosSaker(token: String): List<DigisosSak> {
        val headers = HttpHeaders()
        headers.accept = singletonList(MediaType.APPLICATION_JSON)
        headers.set(AUTHORIZATION, token)
        headers.set("IntegrasjonId", fiksIntegrasjonid)
        headers.set("IntegrasjonPassord", fiksIntegrasjonpassord)
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, HttpEntity<Nothing>(headers), typeRef<List<String>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!.map { s: String -> mapper.readValue(s, DigisosSak::class.java) }
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    override fun hentInformasjonOmKommuneErPaakoblet(kommunenummer: String): KommuneInfo {
        val response = restTemplate.getForEntity("$baseUrl/digisos/api/v1/nav/kommune/$kommunenummer", KommuneInfo::class.java)
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!
        } else {
            log.warn("Noe feilet ved kall til fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }
}

inline fun <reified T : Any> typeRef(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}
