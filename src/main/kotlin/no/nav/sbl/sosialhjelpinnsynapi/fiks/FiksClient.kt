package no.nav.sbl.sosialhjelpinnsynapi.fiks

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.sbl.sosialhjelpinnsynapi.ClientProperties
import no.nav.sbl.sosialhjelpinnsynapi.domain.DigisosSak
import no.nav.sbl.sosialhjelpinnsynapi.domain.KommuneInfo
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException

private val log = LoggerFactory.getLogger(FiksClient::class.java)

private const val digisos_stub_id = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

@Component
class FiksClient(clientProperties: ClientProperties,
                 private val restTemplate: RestTemplate = RestTemplate()) {

    private val baseUrl = clientProperties.fiksDigisosEndpointUrl
    private val mapper = jacksonObjectMapper()


    fun hentDigisosSak(digisosId: String): DigisosSak {
        if (digisosId.equals(digisos_stub_id)) {
            log.info("Hentet stub - digisosId $digisosId")
            return mapper.readValue(ok_digisossak_response, DigisosSak::class.java)
        }

        val response = restTemplate.getForEntity("$baseUrl/digisos/api/v1/soknader/$digisosId", String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            log.info("Hentet DigisosSak $digisosId fra Fiks")
            return mapper.readValue(response.body!!, DigisosSak::class.java)
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    fun hentAlleDigisosSaker(): List<DigisosSak> {
        val response = restTemplate.exchange("$baseUrl/digisos/api/v1/soknader", HttpMethod.GET, null, typeRef<List<String>>())
        if (response.statusCode.is2xxSuccessful) {
            return response.body!!.map { s: String -> mapper.readValue(s, DigisosSak::class.java) }
        } else {
            log.warn("Noe feilet ved kall til Fiks")
            throw ResponseStatusException(response.statusCode, "something went wrong")
        }
    }

    fun hentInformasjonOmKommuneErPaakoblet(kommunenummer: String): KommuneInfo {
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